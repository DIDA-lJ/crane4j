package cn.crane4j.core.support;

import cn.crane4j.core.container.ContainerProvider;
import cn.crane4j.core.container.DefaultContainerManager;
import cn.crane4j.core.container.lifecycle.ContainerRegisterLogger;
import cn.crane4j.core.executor.BeanOperationExecutor;
import cn.crane4j.core.executor.DisorderedBeanOperationExecutor;
import cn.crane4j.core.executor.OrderedBeanOperationExecutor;
import cn.crane4j.core.executor.handler.AssembleOperationHandler;
import cn.crane4j.core.executor.handler.DisassembleOperationHandler;
import cn.crane4j.core.executor.handler.ManyToManyAssembleOperationHandler;
import cn.crane4j.core.executor.handler.OneToManyAssembleOperationHandler;
import cn.crane4j.core.executor.handler.OneToOneAssembleOperationHandler;
import cn.crane4j.core.executor.handler.ReflectiveDisassembleOperationHandler;
import cn.crane4j.core.parser.BeanOperationParser;
import cn.crane4j.core.parser.TypeHierarchyBeanOperationParser;
import cn.crane4j.core.parser.handler.AssembleAnnotationHandler;
import cn.crane4j.core.parser.handler.AssembleEnumAnnotationHandler;
import cn.crane4j.core.parser.handler.AssembleMethodAnnotationHandler;
import cn.crane4j.core.parser.handler.DisassembleAnnotationHandler;
import cn.crane4j.core.parser.handler.strategy.OverwriteMappingStrategy;
import cn.crane4j.core.parser.handler.strategy.OverwriteNotNullMappingStrategy;
import cn.crane4j.core.parser.handler.strategy.PropertyMappingStrategyManager;
import cn.crane4j.core.parser.handler.strategy.ReferenceMappingStrategy;
import cn.crane4j.core.parser.handler.strategy.SimplePropertyMappingStrategyManager;
import cn.crane4j.core.support.container.DefaultMethodContainerFactory;
import cn.crane4j.core.support.container.MethodInvokerContainerCreator;
import cn.crane4j.core.support.converter.ConverterManager;
import cn.crane4j.core.support.converter.HutoolConverterManager;
import cn.crane4j.core.support.reflect.CacheablePropertyOperator;
import cn.crane4j.core.support.reflect.ChainAccessiblePropertyOperator;
import cn.crane4j.core.support.reflect.MapAccessiblePropertyOperator;
import cn.crane4j.core.support.reflect.PropertyOperator;
import cn.crane4j.core.support.reflect.PropertyOperatorHolder;
import cn.crane4j.core.support.reflect.ReflectivePropertyOperator;
import cn.crane4j.core.util.Asserts;
import cn.crane4j.core.util.ConfigurationUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic implementation of {@link Crane4jGlobalConfiguration}.
 *
 * @author huangchengxing
 */
@Getter
public class SimpleCrane4jGlobalConfiguration extends DefaultContainerManager
    implements Crane4jGlobalConfiguration, PropertyMappingStrategyManager {

    @Setter
    private TypeResolver typeResolver;
    @Setter
    private PropertyOperator propertyOperator;
    @Setter
    private ConverterManager converterManager;
    private final Map<String, BeanOperationParser> beanOperationParserMap = new HashMap<>(16);
    private final Map<String, AssembleOperationHandler> assembleOperationHandlerMap = new HashMap<>(4);
    private final Map<String, DisassembleOperationHandler> disassembleOperationHandlerMap = new HashMap<>(4);
    private final Map<String, BeanOperationExecutor> beanOperationExecutorMap = new HashMap<>(4);
    @Delegate
    private final PropertyMappingStrategyManager propertyMappingStrategyManager = new SimplePropertyMappingStrategyManager();

    /**
     * Create a {@link SimpleCrane4jGlobalConfiguration} using the default configuration.
     *
     * @return configuration
     */
    public static SimpleCrane4jGlobalConfiguration create() {
        AnnotationFinder af = SimpleAnnotationFinder.INSTANCE;
        ConverterManager cm = new HutoolConverterManager();
        PropertyOperator operator = new ReflectivePropertyOperator(cm);
        return create(af, cm, operator);
    }

    /**
     * Create a {@link SimpleCrane4jGlobalConfiguration} using the default configuration.
     *
     * @param annotationFinder annotation finder
     * @param converter converter manager
     * @param operator property operator
     * @return configuration
     */
    public static SimpleCrane4jGlobalConfiguration create(
        AnnotationFinder annotationFinder, ConverterManager converter, PropertyOperator operator) {
        SimpleCrane4jGlobalConfiguration configuration = new SimpleCrane4jGlobalConfiguration();
        // basic components
        configuration.setConverterManager(converter);
        operator = new CacheablePropertyOperator(operator);
        operator = new MapAccessiblePropertyOperator(operator);
        operator = new ChainAccessiblePropertyOperator(operator);
        operator = new PropertyOperatorHolder(operator);
        configuration.setPropertyOperator(operator);
        configuration.setTypeResolver(new SimpleTypeResolver());

        // container container lifecycle lifecycle
        Logger logger = LoggerFactory.getLogger(ContainerRegisterLogger.class);
        configuration.registerContainerLifecycleProcessor(new ContainerRegisterLogger(logger::info));

        // operation parser
        configuration.addPropertyMappingStrategy(OverwriteMappingStrategy.INSTANCE);
        configuration.addPropertyMappingStrategy(OverwriteNotNullMappingStrategy.INSTANCE);
        configuration.addPropertyMappingStrategy(new ReferenceMappingStrategy(operator));

        TypeHierarchyBeanOperationParser beanOperationParser = new TypeHierarchyBeanOperationParser();
        AssembleAnnotationHandler assembleAnnotationHandler = new AssembleAnnotationHandler(annotationFinder, configuration, configuration);
        beanOperationParser.addOperationAnnotationHandler(assembleAnnotationHandler);
        AssembleEnumAnnotationHandler assembleEnumAnnotationHandler = new AssembleEnumAnnotationHandler(annotationFinder, configuration, operator, configuration);
        beanOperationParser.addOperationAnnotationHandler(assembleEnumAnnotationHandler);
        DisassembleAnnotationHandler disassembleAnnotationHandler = new DisassembleAnnotationHandler(annotationFinder, configuration);
        beanOperationParser.addOperationAnnotationHandler(disassembleAnnotationHandler);
        MethodInvokerContainerCreator creator = new MethodInvokerContainerCreator(
            configuration.getPropertyOperator(), configuration.getConverterManager()
        );
        AssembleMethodAnnotationHandler annotationHandler = new AssembleMethodAnnotationHandler(
            annotationFinder, configuration,
            Collections.singletonList(new DefaultMethodContainerFactory(creator, annotationFinder)),
            new SimplePropertyMappingStrategyManager()
        );
        beanOperationParser.addOperationAnnotationHandler(annotationHandler);


        configuration.getBeanOperationParserMap().put(BeanOperationParser.class.getSimpleName(), beanOperationParser);
        configuration.getBeanOperationParserMap().put(beanOperationParser.getClass().getSimpleName(), beanOperationParser);

        // operation executor
        DisorderedBeanOperationExecutor disorderedBeanOperationExecutor = new DisorderedBeanOperationExecutor(configuration);
        configuration.getBeanOperationExecutorMap().put(BeanOperationExecutor.class.getSimpleName(), disorderedBeanOperationExecutor);
        configuration.getBeanOperationExecutorMap().put(disorderedBeanOperationExecutor.getClass().getSimpleName(), disorderedBeanOperationExecutor);
        OrderedBeanOperationExecutor orderedBeanOperationExecutor = new OrderedBeanOperationExecutor(configuration, Crane4jGlobalSorter.comparator());
        configuration.getBeanOperationExecutorMap().put(orderedBeanOperationExecutor.getClass().getSimpleName(), orderedBeanOperationExecutor);

        // operation handlers
        OneToOneAssembleOperationHandler oneToOneReflexAssembleOperationHandler = new OneToOneAssembleOperationHandler(operator, converter);
        configuration.getAssembleOperationHandlerMap().put(AssembleOperationHandler.class.getSimpleName(), oneToOneReflexAssembleOperationHandler);
        configuration.getAssembleOperationHandlerMap().put(oneToOneReflexAssembleOperationHandler.getClass().getSimpleName(), oneToOneReflexAssembleOperationHandler);
        OneToManyAssembleOperationHandler oneToManyReflexAssembleOperationHandler = new OneToManyAssembleOperationHandler(operator, converter);
        configuration.getAssembleOperationHandlerMap().put(oneToManyReflexAssembleOperationHandler.getClass().getSimpleName(), oneToManyReflexAssembleOperationHandler);
        ManyToManyAssembleOperationHandler manyToManyReflexAssembleOperationHandler = new ManyToManyAssembleOperationHandler(operator, converter);
        configuration.getAssembleOperationHandlerMap().put(manyToManyReflexAssembleOperationHandler.getClass().getSimpleName(), manyToManyReflexAssembleOperationHandler);
        ReflectiveDisassembleOperationHandler reflectiveDisassembleOperationHandler = new ReflectiveDisassembleOperationHandler(operator);
        configuration.getDisassembleOperationHandlerMap().put(DisassembleOperationHandler.class.getSimpleName(), reflectiveDisassembleOperationHandler);
        configuration.getDisassembleOperationHandlerMap().put(reflectiveDisassembleOperationHandler.getClass().getSimpleName(), reflectiveDisassembleOperationHandler);

        // container providers
        configuration.registerContainerProvider(configuration.getClass().getSimpleName(), configuration);
        configuration.registerContainerProvider(ContainerProvider.class.getSimpleName(), configuration);

        return configuration;
    }

    /**
     * Get bean operation executor.
     *
     * @param executorName executor name
     * @param executorType executor type
     * @return executor
     */
    @NonNull
    @Override
    public BeanOperationExecutor getBeanOperationExecutor(
        @Nullable String executorName, Class<?> executorType) {
        BeanOperationExecutor executor = ConfigurationUtil.getComponentFromConfiguration(
            BeanOperationExecutor.class, executorType, executorName,
            (t, n) -> {
                BeanOperationExecutor r = beanOperationExecutorMap.get(n);
                return t.isAssignableFrom(r.getClass()) ? r : null;
            },
            t -> beanOperationExecutorMap.get(t.getSimpleName())
        );
        Asserts.isNotNull(executor, "cannot find executor [{}]({})", executorName, executorType);
        return executor;
    }

    /**
     * Get bean operation parser.
     *
     * @param parserName parser name
     * @param parserType parser type
     * @return parser
     */
    @NonNull
    @Override
    public BeanOperationParser getBeanOperationsParser(@Nullable String parserName, Class<?> parserType) {
        BeanOperationParser parser = ConfigurationUtil.getComponentFromConfiguration(
            BeanOperationParser.class, parserType, parserName,
            (t, n) -> {
                BeanOperationParser r = beanOperationParserMap.get(n);
                return t.isAssignableFrom(r.getClass()) ? r : null;
            },
            t -> beanOperationParserMap.get(t.getSimpleName())
        );
        Asserts.isNotNull(parser, "cannot find parser [{}]({})", parserName, parserType);
        return parser;
    }

    /**
     * Get assemble operation handler.
     *
     * @param handlerName handler name
     * @param handlerType handler type
     * @return handler
     */
    @NonNull
    @Override
    public AssembleOperationHandler getAssembleOperationHandler(@Nullable String handlerName, Class<?> handlerType) {
        AssembleOperationHandler parser = ConfigurationUtil.getComponentFromConfiguration(
            AssembleOperationHandler.class, handlerType, handlerName,
            (t, n) -> {
                AssembleOperationHandler r = assembleOperationHandlerMap.get(n);
                return t.isAssignableFrom(r.getClass()) ? r : null;
            },
            t -> assembleOperationHandlerMap.get(t.getSimpleName())
        );
        Asserts.isNotNull(parser, "cannot find assemble handler [{}]({})", handlerName, handlerType);
        return parser;
    }

    /**
     * Get assemble operation handler.
     *
     * @param handlerName handler name
     * @param handlerType handler type
     * @return handler
     */
    @NonNull
    @Override
    public DisassembleOperationHandler getDisassembleOperationHandler(@Nullable String handlerName, Class<?> handlerType) {
        DisassembleOperationHandler parser = ConfigurationUtil.getComponentFromConfiguration(
            DisassembleOperationHandler.class, handlerType, handlerName,
            (t, n) -> {
                DisassembleOperationHandler r = disassembleOperationHandlerMap.get(n);
                return t.isAssignableFrom(r.getClass()) ? r : null;
            },
            t -> disassembleOperationHandlerMap.get(t.getSimpleName())
        );
        Asserts.isNotNull(parser, "cannot find disassemble handler [{}]({})", handlerName, handlerType);
        return parser;
    }
}
