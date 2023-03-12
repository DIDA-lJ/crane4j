package cn.crane4j.springboot.support;

import cn.crane4j.core.cache.Cache;
import cn.crane4j.core.cache.CacheManager;
import cn.crane4j.core.container.CacheableContainer;
import cn.crane4j.core.container.Container;
import cn.crane4j.core.container.ContainerProvider;
import cn.crane4j.core.support.callback.ContainerRegisterAware;
import cn.crane4j.springboot.config.Crane4jProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * <p>The default implementation of {@link ContainerRegisterAware}. <br />
 * Used to wrap the container as a cacheable container
 * according to {@link Crane4jProperties#getCacheContainers()} configuration before registering it.
 *
 * @author huangchengxing
 */
public class DefaultCacheableContainerProcessor implements ContainerRegisterAware {

    private final CacheManager cacheManager;
    private final Map<String, String> containerConfigs;

    /**
     * Create a {@link DefaultCacheableContainerProcessor} instance.
     *
     * @param cacheManager cache manager
     * @param config config list of cache name and container
     */
    public DefaultCacheableContainerProcessor(CacheManager cacheManager, Map<String, String> config) {
        this.cacheManager = cacheManager;
        this.containerConfigs = config;
    }

    /**
     * Called before {@link Container} is registered to {@link ContainerProvider}.<br />
     * If the return value is {@code null}, the registration of the container will be abandoned
     *
     * @param operator  caller of the current method
     * @param container container
     * @return {@link Container} who really wants to be registered to {@link ContainerProvider}
     */
    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public Container<?> beforeContainerRegister(Object operator, @Nonnull Container<?> container) {
        String cacheName = containerConfigs.get(container.getNamespace());
        if (Objects.nonNull(cacheName)) {
            Cache<Object> cache = cacheManager.getCache(cacheName);
            return new CacheableContainer<>((Container<Object>)container, cache);
        }
        return container;
    }
}
