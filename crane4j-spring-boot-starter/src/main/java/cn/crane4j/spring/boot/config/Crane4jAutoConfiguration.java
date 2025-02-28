package cn.crane4j.spring.boot.config;

import cn.crane4j.annotation.ContainerConstant;
import cn.crane4j.annotation.ContainerEnum;
import cn.crane4j.annotation.ContainerMethod;
import cn.crane4j.core.cache.CacheManager;
import cn.crane4j.core.cache.ConcurrentMapCacheManager;
import cn.crane4j.core.container.ContainerManager;
import cn.crane4j.core.container.Containers;
import cn.crane4j.core.container.lifecycle.CacheableContainerProcessor;
import cn.crane4j.core.container.lifecycle.ContainerInstanceLifecycleProcessor;
import cn.crane4j.core.container.lifecycle.ContainerRegisterLogger;
import cn.crane4j.core.executor.BeanOperationExecutor;
import cn.crane4j.core.executor.DisorderedBeanOperationExecutor;
import cn.crane4j.core.executor.OrderedBeanOperationExecutor;
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
import cn.crane4j.core.parser.handler.OperationAnnotationHandler;
import cn.crane4j.core.parser.handler.strategy.OverwriteMappingStrategy;
import cn.crane4j.core.parser.handler.strategy.OverwriteNotNullMappingStrategy;
import cn.crane4j.core.parser.handler.strategy.PropertyMappingStrategy;
import cn.crane4j.core.parser.handler.strategy.PropertyMappingStrategyManager;
import cn.crane4j.core.parser.handler.strategy.ReferenceMappingStrategy;
import cn.crane4j.core.parser.handler.strategy.SimplePropertyMappingStrategyManager;
import cn.crane4j.core.parser.operation.AssembleOperation;
import cn.crane4j.core.support.AnnotationFinder;
import cn.crane4j.core.support.ContainerAdapterRegister;
import cn.crane4j.core.support.Crane4jGlobalConfiguration;
import cn.crane4j.core.support.DefaultContainerAdapterRegister;
import cn.crane4j.core.support.OperateTemplate;
import cn.crane4j.core.support.ParameterNameFinder;
import cn.crane4j.core.support.SimpleTypeResolver;
import cn.crane4j.core.support.TypeResolver;
import cn.crane4j.core.support.auto.AutoOperateAnnotatedElementResolver;
import cn.crane4j.core.support.auto.ClassBasedAutoOperateAnnotatedElementResolver;
import cn.crane4j.core.support.auto.ComposableAutoOperateAnnotatedElementResolver;
import cn.crane4j.core.support.auto.MethodBasedAutoOperateAnnotatedElementResolver;
import cn.crane4j.core.support.container.CacheableMethodContainerFactory;
import cn.crane4j.core.support.container.DefaultMethodContainerFactory;
import cn.crane4j.core.support.container.MethodContainerFactory;
import cn.crane4j.core.support.container.MethodInvokerContainerCreator;
import cn.crane4j.core.support.converter.ConverterManager;
import cn.crane4j.core.support.expression.ExpressionEvaluator;
import cn.crane4j.core.support.expression.MethodBaseExpressionExecuteDelegate;
import cn.crane4j.core.support.operator.DefaultOperatorProxyMethodFactory;
import cn.crane4j.core.support.operator.DynamicContainerOperatorProxyMethodFactory;
import cn.crane4j.core.support.operator.OperatorProxyFactory;
import cn.crane4j.core.support.operator.OperatorProxyMethodFactory;
import cn.crane4j.core.support.reflect.AsmReflectivePropertyOperator;
import cn.crane4j.core.support.reflect.CacheablePropertyOperator;
import cn.crane4j.core.support.reflect.ChainAccessiblePropertyOperator;
import cn.crane4j.core.support.reflect.MapAccessiblePropertyOperator;
import cn.crane4j.core.support.reflect.PropertyOperator;
import cn.crane4j.core.support.reflect.PropertyOperatorHolder;
import cn.crane4j.core.support.reflect.ReflectivePropertyOperator;
import cn.crane4j.core.util.CollectionUtils;
import cn.crane4j.core.util.StringUtils;
import cn.crane4j.extension.spring.BeanAwareAssembleMethodAnnotationHandler;
import cn.crane4j.extension.spring.BeanMethodContainerRegistrar;
import cn.crane4j.extension.spring.Crane4jApplicationContext;
import cn.crane4j.extension.spring.MergedAnnotationFinder;
import cn.crane4j.extension.spring.ResolvableExpressionEvaluator;
import cn.crane4j.extension.spring.SpringConverterManager;
import cn.crane4j.extension.spring.SpringParameterNameFinder;
import cn.crane4j.extension.spring.ValueResolveAssembleAnnotationHandler;
import cn.crane4j.extension.spring.aop.MethodArgumentAutoOperateAdvisor;
import cn.crane4j.extension.spring.aop.MethodResultAutoOperateAdvisor;
import cn.crane4j.extension.spring.expression.SpelExpressionContext;
import cn.crane4j.extension.spring.expression.SpelExpressionEvaluator;
import cn.crane4j.extension.spring.scanner.ClassScanner;
import cn.crane4j.extension.spring.scanner.ScannedContainerRegistrar;
import cn.crane4j.extension.spring.util.ContainerResolveUtils;
import cn.crane4j.spring.boot.annotation.EnableCrane4j;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringValueResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>The automatic configuration class of crane.<br />
 * Inject the current configuration class into the Spring container,
 * or make it effective directly through {@link EnableCrane4j} annotation,
 * which will automatically assemble various components required by the crane runtime.
 *
 * @author huangchengxing
 * @see cn.crane4j.extension.spring
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(Crane4jAutoConfiguration.Properties.class)
public class Crane4jAutoConfiguration {

    public static final String CRANE_PREFIX = "crane4j";

    // region ======= basic =======

    @ConditionalOnMissingBean(ClassScanner.class)
    @Bean
    public ClassScanner classScanner() {
        return ClassScanner.INSTANCE;
    }

    @ConditionalOnBean(ConversionService.class)
    @ConditionalOnMissingBean(ConverterManager.class)
    @Bean
    public SpringConverterManager springConverterManager(ConversionService conversionService) {
        return new SpringConverterManager(conversionService);
    }

    @ConditionalOnMissingBean({ConversionService.class, ConverterManager.class})
    @Bean
    public SpringConverterManager newSpringConverterManager() {
        log.info("No ConversionService or ConverterManager bean found, use default ConversionService.");
        return new SpringConverterManager(DefaultConversionService.getSharedInstance());
    }

    @Primary
    @ConditionalOnMissingBean
    @Bean
    public Crane4jApplicationContext crane4jApplicationContext(ApplicationContext applicationContext) {
        return new Crane4jApplicationContext(applicationContext);
    }

    @ConditionalOnMissingBean
    @Bean
    public ScannedContainerRegistrar scannedContainerRegister() {
        return new ScannedContainerRegistrar();
    }

    @Bean
    public PropertyOperator propertyOperator(Properties properties, ConverterManager converterManager) {
        // reflect asm may only support jdk 1.8
        PropertyOperator operator = properties.isEnableAsmReflect() && System.getProperty("java.version").contains("1.8") ?
            new AsmReflectivePropertyOperator(converterManager) : new ReflectivePropertyOperator(converterManager);
        operator = new CacheablePropertyOperator(operator);
        if (properties.isEnableMapOperate()) {
            operator = new MapAccessiblePropertyOperator(operator);
        }
        if (properties.isEnableChainOperate()) {
            operator = new ChainAccessiblePropertyOperator(operator);
        }
        return new PropertyOperatorHolder(operator);
    }

    @ConditionalOnMissingBean(AnnotationFinder.class)
    @Bean
    public MergedAnnotationFinder mergedAnnotationFinder() {
        return new MergedAnnotationFinder();
    }

    @ConditionalOnMissingBean(TypeResolver.class)
    @Bean
    public SimpleTypeResolver simpleTypeResolver() {
        return new SimpleTypeResolver();
    }

    @ConditionalOnMissingBean(ExpressionEvaluator.class)
    @Bean
    public SpelExpressionEvaluator spelExpressionEvaluator() {
        return new SpelExpressionEvaluator(new SpelExpressionParser());
    }

    @ConditionalOnMissingBean(CacheManager.class)
    @Bean
    public ConcurrentMapCacheManager concurrentMapCacheManager() {
        return new ConcurrentMapCacheManager(CollectionUtils::newWeakConcurrentMap);
    }

    @Order(0)
    @Bean
    public ContainerInstanceLifecycleProcessor containerInstanceLifecycleProcessor() {
        return new ContainerInstanceLifecycleProcessor();
    }

    @Order(1)
    @Bean
    public ContainerRegisterLogger containerRegisterLogger() {
        Logger logger = LoggerFactory.getLogger(ContainerRegisterLogger.class);
        return new ContainerRegisterLogger(logger::debug);
    }

    @Order(2)
    @ConditionalOnMissingBean
    @ConditionalOnBean(CacheManager.class)
    @Bean
    public CacheableContainerProcessor cacheableContainerProcessor(CacheManager cacheManager, Properties properties) {
        Map<String, String> cacheMap = new HashMap<>(16);
        properties.getCacheContainers().forEach((cacheName, namespaces) ->
            namespaces.forEach(namespace -> cacheMap.put(namespace, cacheName))
        );
        CacheableContainerProcessor processor = new CacheableContainerProcessor(cacheManager);
        processor.setCacheNameSelector((definition, container) -> cacheMap.get(container.getNamespace()));
        return processor;
    }

    @ConditionalOnMissingBean
    @Bean
    public OperateTemplate operateTemplate(
        BeanOperationParser parser, BeanOperationExecutor executor, TypeResolver typeResolver) {
        return new OperateTemplate(parser, executor, typeResolver);
    }

    @ConditionalOnMissingBean(ParameterNameFinder.class)
    @Bean
    public SpringParameterNameFinder springParameterNameFinder() {
        return new SpringParameterNameFinder(new DefaultParameterNameDiscoverer());
    }

    @Bean
    public DefaultContainerAdapterRegister defaultContainerAdapterRegister() {
        return DefaultContainerAdapterRegister.INSTANCE;
    }

    @ConditionalOnMissingBean(MethodBasedAutoOperateAnnotatedElementResolver.class)
    @Bean
    public MethodBasedAutoOperateAnnotatedElementResolver methodBasedAutoOperateAnnotatedElementResolver(
        Crane4jGlobalConfiguration crane4jGlobalConfiguration, TypeResolver typeResolver) {
        return new MethodBasedAutoOperateAnnotatedElementResolver(crane4jGlobalConfiguration, typeResolver);
    }

    @ConditionalOnMissingBean(ClassBasedAutoOperateAnnotatedElementResolver.class)
    @Bean
    public ClassBasedAutoOperateAnnotatedElementResolver classBasedAutoOperateAnnotatedElementResolver(
        Crane4jGlobalConfiguration crane4jGlobalConfiguration, ExpressionEvaluator expressionEvaluator) {
        return new ClassBasedAutoOperateAnnotatedElementResolver(
            crane4jGlobalConfiguration, expressionEvaluator, SpelExpressionContext::new
        );
    }

    @Primary
    @Bean
    public ComposableAutoOperateAnnotatedElementResolver composableAutoOperateAnnotatedElementResolver(
        Collection<AutoOperateAnnotatedElementResolver> autoOperateAnnotatedElementResolvers) {
        return new ComposableAutoOperateAnnotatedElementResolver(new ArrayList<>(autoOperateAnnotatedElementResolvers));
    }

    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = CRANE_PREFIX,
        name = "enable-method-container",
        havingValue = "true", matchIfMissing = true
    )
    @Bean
    public BeanMethodContainerRegistrar beanMethodContainerPostProcessor(
        AnnotationFinder annotationFinder, Collection<MethodContainerFactory> factories, Crane4jGlobalConfiguration configuration) {
        return new BeanMethodContainerRegistrar(factories, annotationFinder, configuration);
    }

    @ConditionalOnMissingBean
    @Bean
    public Crane4jInitializer crane4jInitializer(
        ClassScanner classScanner, PropertyOperator propertyOperator,
        ApplicationContext applicationContext, AnnotationFinder annotationFinder,
        Crane4jGlobalConfiguration configuration, Properties properties) {
        return new Crane4jInitializer(
            propertyOperator, applicationContext, properties, annotationFinder,
            configuration, classScanner
        );
    }

    // endregion



    // region ======= operation parse =======

    @ConditionalOnMissingBean
    @Bean
    public SimplePropertyMappingStrategyManager simplePropertyMappingStrategyManager(
        Collection<PropertyMappingStrategy> propertyMappingStrategies) {
        SimplePropertyMappingStrategyManager manager = new SimplePropertyMappingStrategyManager();
        propertyMappingStrategies.forEach(manager::addPropertyMappingStrategy);
        return manager;
    }

    @Bean
    public OverwriteNotNullMappingStrategy overwriteNotNullMappingStrategy() {
        return OverwriteNotNullMappingStrategy.INSTANCE;
    }

    @Bean
    public OverwriteMappingStrategy overwriteMappingStrategy() {
        return OverwriteMappingStrategy.INSTANCE;
    }

    @Bean
    public ReferenceMappingStrategy referenceMappingStrategy(PropertyOperator propertyOperator) {
        return new ReferenceMappingStrategy(propertyOperator);
    }

    @ConditionalOnMissingBean(BeanResolver.class)
    @Bean
    public BeanFactoryResolver beanFactoryResolver(ApplicationContext applicationContext) {
        return new BeanFactoryResolver(applicationContext);
    }

    @ConditionalOnMissingBean(AssembleAnnotationHandler.class)
    @Bean
    public ValueResolveAssembleAnnotationHandler valueResolveAssembleAnnotationHandler(
        AnnotationFinder annotationFinder, Crane4jGlobalConfiguration configuration,
        ExpressionEvaluator evaluator, BeanResolver beanResolver,
        PropertyMappingStrategyManager propertyMappingStrategyManager) {
        return new ValueResolveAssembleAnnotationHandler(
            annotationFinder, configuration, evaluator, beanResolver, propertyMappingStrategyManager
        );
    }

    @ConditionalOnMissingBean(AssembleMethodAnnotationHandler.class)
    @Bean
    public BeanAwareAssembleMethodAnnotationHandler beanAwareAssembleMethodAnnotationHandler(
        AnnotationFinder annotationFinder, Crane4jGlobalConfiguration globalConfiguration,
        PropertyMappingStrategyManager propertyMappingStrategyManager,
        Collection<MethodContainerFactory> methodContainerFactories,
        ApplicationContext applicationContext) {
        return new BeanAwareAssembleMethodAnnotationHandler(
            annotationFinder, globalConfiguration, methodContainerFactories, applicationContext, propertyMappingStrategyManager
        );
    }

    @ConditionalOnMissingBean
    @Bean
    public AssembleEnumAnnotationHandler assembleEnumAnnotationHandler(
        AnnotationFinder annotationFinder, Crane4jGlobalConfiguration globalConfiguration,
        PropertyOperator propertyOperator, PropertyMappingStrategyManager propertyMappingStrategyManager) {
        return new AssembleEnumAnnotationHandler(annotationFinder, globalConfiguration, propertyOperator, propertyMappingStrategyManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public DisassembleAnnotationHandler disassembleAnnotationHandler(
        AnnotationFinder annotationFinder, Crane4jGlobalConfiguration configuration) {
        return new DisassembleAnnotationHandler(annotationFinder, configuration);
    }

    @ConditionalOnMissingBean
    @Bean
    public TypeHierarchyBeanOperationParser typeHierarchyBeanOperationParser(Collection<OperationAnnotationHandler> resolvers) {
        TypeHierarchyBeanOperationParser parser = new TypeHierarchyBeanOperationParser();
        resolvers.forEach(parser::addOperationAnnotationHandler);
        return parser;
    }

    // endregion

    // region ======= operation execute =======

    @Primary
    @ConditionalOnMissingBean
    @Bean
    public DisorderedBeanOperationExecutor disorderedBeanOperationExecutor(ContainerManager containerManager) {
        return new DisorderedBeanOperationExecutor(containerManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public OrderedBeanOperationExecutor orderedBeanOperationExecutor(ContainerManager containerManager) {
        return new OrderedBeanOperationExecutor(containerManager, Comparator.comparing(AssembleOperation::getSort));
    }

    @ConditionalOnMissingBean
    @Bean
    public MethodInvokerContainerCreator methodInvokerContainerCreator(PropertyOperator propertyOperator, ConverterManager converterManager) {
        return new MethodInvokerContainerCreator(propertyOperator, converterManager);
    }

    @Order
    @Bean
    public DefaultMethodContainerFactory defaultMethodContainerFactory(
        MethodInvokerContainerCreator methodInvokerContainerCreator, AnnotationFinder annotationFinder) {
        return new DefaultMethodContainerFactory(methodInvokerContainerCreator, annotationFinder);
    }

    @Order(Ordered.LOWEST_PRECEDENCE - 1)
    @ConditionalOnBean(CacheManager.class)
    @Bean
    public CacheableMethodContainerFactory cacheableMethodContainerFactory(
        CacheManager cacheManager, MethodInvokerContainerCreator methodInvokerContainerCreator, AnnotationFinder annotationFinder) {
        return new CacheableMethodContainerFactory(methodInvokerContainerCreator, annotationFinder, cacheManager);
    }

    @Primary
    @Bean
    public OneToOneAssembleOperationHandler oneToOneAssembleOperationHandler(
        PropertyOperator propertyOperator, ConverterManager converterManager) {
        return new OneToOneAssembleOperationHandler(propertyOperator, converterManager);
    }

    @Bean
    public ManyToManyAssembleOperationHandler manyToManyAssembleOperationHandler(
        PropertyOperator propertyOperator, ConverterManager converterManager) {
        return new ManyToManyAssembleOperationHandler(propertyOperator, converterManager);
    }

    @Bean
    public OneToManyAssembleOperationHandler oneToManyAssembleOperationHandler(
        PropertyOperator propertyOperator, ConverterManager converterManager) {
        return new OneToManyAssembleOperationHandler(propertyOperator, converterManager);
    }

    @Primary
    @ConditionalOnMissingBean
    @Bean
    public ReflectiveDisassembleOperationHandler reflectiveDisassembleOperationHandler(PropertyOperator propertyOperator) {
        return new ReflectiveDisassembleOperationHandler(propertyOperator);
    }

    // endregion

    // region ======= operator interface =======

    @ConditionalOnMissingBean(OperatorProxyMethodFactory.class)
    @Bean
    public OperatorProxyFactory operatorProxyFactory(
        AnnotationFinder annotationFinder, Crane4jGlobalConfiguration configuration,
        Collection<OperatorProxyMethodFactory> factories) {
        OperatorProxyFactory factory = new OperatorProxyFactory(configuration, annotationFinder);
        factories.forEach(factory::addProxyMethodFactory);
        return factory;
    }

    @ConditionalOnBean(OperatorProxyFactory.class)
    @ConditionalOnMissingBean
    @Bean
    public DefaultOperatorProxyMethodFactory defaultProxyMethodFactory(ConverterManager converterManager) {
        return new DefaultOperatorProxyMethodFactory(converterManager);
    }

    @ConditionalOnBean(OperatorProxyFactory.class)
    @ConditionalOnMissingBean
    @Order
    @Bean
    public DynamicContainerOperatorProxyMethodFactory dynamicContainerOperatorProxyMethodFactory(
        ConverterManager converterManager, ParameterNameFinder parameterNameFinder,
        AnnotationFinder annotationFinder, ContainerAdapterRegister containerAdapterRegister) {
        return new DynamicContainerOperatorProxyMethodFactory(
            converterManager, parameterNameFinder, annotationFinder, containerAdapterRegister
        );
    }

    // endregion

    // region ======= auto operate =======

    @ConditionalOnMissingBean
    @Bean
    public ResolvableExpressionEvaluator resolvableExpressionEvaluator(
        ExpressionEvaluator expressionEvaluator, ParameterNameFinder parameterNameFinder, BeanResolver beanResolver) {
        return new ResolvableExpressionEvaluator(
            parameterNameFinder, expressionEvaluator, method -> {
            SpelExpressionContext context = new SpelExpressionContext(method);
            context.setBeanResolver(beanResolver);
            return context;
        }
        );
    }

    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = CRANE_PREFIX,
        name = "enable-method-result-auto-operate",
        havingValue = "true", matchIfMissing = true
    )
    @Bean
    public MethodResultAutoOperateAdvisor methodResultAutoOperateAdvisor(
        AutoOperateAnnotatedElementResolver autoOperateAnnotatedElementResolver,
        ResolvableExpressionEvaluator resolvableExpressionEvaluator) {
        return new MethodResultAutoOperateAdvisor(autoOperateAnnotatedElementResolver, resolvableExpressionEvaluator);
    }

    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = CRANE_PREFIX,
        name = "enable-method-argument-auto-operate",
        havingValue = "true", matchIfMissing = true
    )
    @Bean
    public MethodArgumentAutoOperateAdvisor methodArgumentAutoOperateAdvisor(
        MethodBaseExpressionExecuteDelegate methodBaseExpressionExecuteDelegate,
        AutoOperateAnnotatedElementResolver autoOperateAnnotatedElementResolver,
        ParameterNameFinder parameterNameDiscoverer, AnnotationFinder annotationFinder) {
        return new MethodArgumentAutoOperateAdvisor(autoOperateAnnotatedElementResolver,
            methodBaseExpressionExecuteDelegate,
            parameterNameDiscoverer, annotationFinder
        );
    }

    // endregion

    /**
     * Configurable properties.
     *
     * @author huangchengxing
     * @see Crane4jInitializer
     */
    @ConfigurationProperties(prefix = CRANE_PREFIX)
    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Properties {

        /**
         * <p>Whether to enable objects of type {@link Map} be used as target objects or data source objects.<br />
         * <b><NOTE</b>:If the customized {@link PropertyOperator} is registered, the configuration will be overwritten.
         *
         * @see MapAccessiblePropertyOperator
         */
        private boolean enableMapOperate = true;

        /**
         * <p>Whether to enable accessing object properties through chain operators.<br />
         * <b><NOTE</b>:If the customized {@link PropertyOperator} is registered, the configuration will be overwritten.
         *
         * @see MapAccessiblePropertyOperator
         */
        private boolean enableChainOperate = true;

        /**
         * <p>Whether to enable reflection enhancement based on {@link com.esotericsoftware.reflectasm}.
         * Enabling can improve performance to some extent, but may increase additional memory usage.<br />
         * <b><NOTE</b>:If the customized {@link PropertyOperator} is registered, the configuration will be overwritten.
         *
         * @see AsmReflectivePropertyOperator
         */
        private boolean enableAsmReflect = false;

        /**
         * <p>Scan the specified package path, adapt the enumeration
         * under the path and register it as a data source container.<br />
         * For example: {@code com.example.instant.enum.*}.
         *
         * <p>If only need to scan the enumeration annotated by
         * {@link ContainerEnum}, set {@link #onlyLoadAnnotatedEnum} is {@code true}.
         *
         * @see ContainerEnum
         * @see Containers#forEnum
         */
        private Set<String> containerEnumPackages = new LinkedHashSet<>();

        /**
         * Whether to load only the enumeration annotated by {@link ContainerEnum}.
         */
        private boolean onlyLoadAnnotatedEnum = false;

        /**
         * <p>Scan the specified package path, adapt the constant class annotated by
         * {@link ContainerConstant} under the path and register it as a data source container.<br />
         * For example: {@code com.example.instant.enum.*}.
         *
         * @see ContainerConstant
         * @see Containers#forConstantClass
         */
        private Set<String> containerConstantPackages = new LinkedHashSet<>();

        /**
         * <p>Scan all classes under the specified package path and pre-parse them
         * using the configuration parser in the spring context.<br />
         * For example: {@code com.example.entity.*}.
         *
         * <p>This configuration is conducive to improving the efficiency
         * of some configuration parsers with cache function.
         *
         * @see BeanOperationParser
         */
        private Set<String> operateEntityPackages = new LinkedHashSet<>();

        /**
         * Whether to enable automatic filling of aspect with method parameters.
         *
         * @see MethodArgumentAutoOperateAdvisor
         */
        private boolean enableMethodArgumentAutoOperate = true;

        /**
         * Whether to enable the method return value to automatically fill the cut surface.
         *
         * @see MethodResultAutoOperateAdvisor
         */
        private boolean enableMethodResultAutoOperate = true;

        /**
         * Whether to automatically scan and register the method
         * annotated by {@link ContainerMethod} as the data source container.
         *
         * @see BeanMethodContainerRegistrar
         */
        private boolean enableMethodContainer = true;

        /**
         * Declare which data sources need to be packaged as caches in the format {@code cache name: namespace of container}.
         */
        private Map<String, Set<String>> cacheContainers = new LinkedHashMap<>();
    }

    /**
     * The default initializer is used to initialize some caches or components after the application is started.
     *
     * @author huangchengxing
     */
    @Slf4j
    @RequiredArgsConstructor
    public static class Crane4jInitializer implements Ordered, ApplicationRunner, EmbeddedValueResolverAware {

        public static final int CRANE4J_INITIALIZER_ORDER = 0;

        private final PropertyOperator propertyOperator;
        private final ApplicationContext applicationContext;
        private final Properties properties;
        private final AnnotationFinder annotationFinder;
        private final Crane4jGlobalConfiguration configuration;
        private final ClassScanner classScanner;
        @Setter
        private StringValueResolver embeddedValueResolver;

        /**
         * Get the order value of this object.
         * <p>Higher values are interpreted as lower priority. As a consequence,
         * the object with the lowest value has the highest priority (somewhat
         * analogous to Servlet {@code load-on-startup} values).
         * <p>Same order values will result in arbitrary sort positions for the
         * affected objects.
         *
         * @return the order value
         * @see #HIGHEST_PRECEDENCE
         * @see #LOWEST_PRECEDENCE
         */
        @Override
        public int getOrder() {
            return CRANE4J_INITIALIZER_ORDER;
        }

        @SneakyThrows
        @Override
        public void run(ApplicationArguments args) {
            log.info("crane4j components initializing......");
            // load enumeration and register it as a container
            loadContainerEnum();
            // load a constant class and register it as a container
            loadConstantClass();
            // pre resolution class operation configuration
            loadOperateEntity();
            log.info("crane4j components initialization completed!");
        }

        private void loadConstantClass() {
            ContainerResolveUtils.loadConstantClass(
                loadTypes(properties.getContainerConstantPackages()), configuration, annotationFinder
            );
        }

        private void loadContainerEnum() {
            ContainerResolveUtils.loadContainerEnum(
                loadTypes(properties.getContainerEnumPackages()), properties.isOnlyLoadAnnotatedEnum(),
                configuration, annotationFinder, propertyOperator
            );
        }

        private void loadOperateEntity() {
            loadTypes(properties.getOperateEntityPackages())
                .forEach(type -> applicationContext.getBeansOfType(BeanOperationParser.class)
                    .values()
                    .forEach(parser -> parser.parse(type))
                );
        }

        private Set<Class<?>> loadTypes(Collection<String> packages) {
            return packages.stream()
                .map(embeddedValueResolver::resolveStringValue)
                .filter(StringUtils::isNotEmpty)
                .map(classScanner::scan)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        }
    }
}
