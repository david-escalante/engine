/*
 * Copyright (C) 2007-2013 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.engine.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.craftercms.core.processors.ItemProcessor;
import org.craftercms.core.processors.impl.ItemProcessorPipeline;
import org.craftercms.core.service.Content;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Item;
import org.craftercms.core.service.ItemFilter;
import org.craftercms.core.service.Tree;
import org.craftercms.core.service.impl.CompositeItemFilter;
import org.craftercms.engine.model.SiteItem;
import org.craftercms.engine.model.converters.ModelValueConverter;
import org.craftercms.engine.service.SiteItemService;
import org.craftercms.engine.service.context.SiteContext;
import org.craftercms.engine.service.filter.ExcludeByNameItemFilter;
import org.craftercms.engine.service.filter.ExpectedNodeValueItemFilter;
import org.craftercms.engine.service.filter.IncludeByNameItemFilter;
import org.springframework.beans.factory.annotation.Required;

/**
 * Default implementation of {@link SiteItemService}.
 *
 * @author Alfonso Vásquez
 */
public class SiteItemServiceImpl implements SiteItemService {

    protected ContentStoreService storeService;
    protected List<ItemFilter> defaultFilters;
    protected List<ItemProcessor> defaultProcessors;
    protected Map<String, ModelValueConverter<?>> modelValueConverters;
    protected Comparator<SiteItem> sortComparator;

    @Required
    public void setStoreService(ContentStoreService storeService) {
        this.storeService = storeService;
    }

    public void setDefaultFilters(List<ItemFilter> defaultFilters) {
        this.defaultFilters = defaultFilters;
    }

    public void setDefaultProcessors(List<ItemProcessor> defaultProcessors) {
        this.defaultProcessors = defaultProcessors;
    }

    @Required
    public void setModelValueConverters(Map<String, ModelValueConverter<?>> modelValueConverters) {
        this.modelValueConverters = modelValueConverters;
    }

    public void setSortComparator(Comparator<SiteItem> sortComparator) {
        this.sortComparator = sortComparator;
    }

    @Override
    public SiteItem getSiteItem(String url) {
        return getSiteItem(url, null);
    }

    @Override
    public SiteItem getSiteItem(String url, ItemProcessor processor) {
        if (CollectionUtils.isNotEmpty(defaultProcessors)) {
            ItemProcessorPipeline processorPipeline = new ItemProcessorPipeline(new ArrayList<>(defaultProcessors));

            if (processor != null) {
                processorPipeline.addProcessor(processor);
            }

            processor = processorPipeline;
        }

        Item item = storeService.findItem(getSiteContext().getContext(), null, url, processor);
        if (item != null) {
            return createItemWrapper(item);
        } else {
            return null;
        }
    }

    @Override
    public SiteItem getSiteTree(String url, int depth)  {
        return getSiteTree(url, depth, null, (ItemProcessor)null);
    }

    @Override
    public SiteItem getSiteTree(String url, int depth, ItemFilter filter, ItemProcessor processor) {
        if (CollectionUtils.isNotEmpty(defaultFilters)) {
            CompositeItemFilter compositeFilter = new CompositeItemFilter(new ArrayList<>(defaultFilters));

            if (filter != null) {
                compositeFilter.addFilter(filter);
            }

            filter = compositeFilter;
        }

        if (CollectionUtils.isNotEmpty(defaultProcessors)) {
            ItemProcessorPipeline processorPipeline = new ItemProcessorPipeline(new ArrayList<>(defaultProcessors));

            if (processor != null) {
                processorPipeline.addProcessor(processor);
            }

            processor = processorPipeline;
        }

        Tree tree = storeService.findTree(getSiteContext().getContext(), null, url, depth, filter, processor);
        if (tree != null) {
            return createItemWrapper(tree);
        } else {
            return null;
        }
    }

    @Override
    public Content getRawContent(String url) {
        return storeService.findContent(getSiteContext().getContext(), url);
    }

    @Deprecated
    @Override
    public SiteItem getSiteTree(String url, int depth, String includeByNameRegex, String excludeByNameRegex) {
        return getSiteTree(url, depth, includeByNameRegex, excludeByNameRegex, (Map<String, String>)null);
    }

    @Deprecated
    @Override
    public SiteItem getSiteTree(String url, int depth, String includeByNameRegex, String excludeByNameRegex,
                                String[]... nodeXPathAndExpectedValuePairs) {
        Map<String, String> nodeXPathAndExpectedValuePairMap = null;

        if (ArrayUtils.isNotEmpty(nodeXPathAndExpectedValuePairs)) {
            nodeXPathAndExpectedValuePairMap = new HashMap<>();

            for (String[] nodeXPathAndExpectedValuePair : nodeXPathAndExpectedValuePairs) {
                String nodeXPathQuery = nodeXPathAndExpectedValuePair[0];
                String expectedNodeValueRegex = nodeXPathAndExpectedValuePair[1];

                nodeXPathAndExpectedValuePairMap.put(nodeXPathQuery, expectedNodeValueRegex);
            }
        }

        return getSiteTree(url, depth, includeByNameRegex, excludeByNameRegex, nodeXPathAndExpectedValuePairMap);
    }

    @Deprecated
    @Override
    public SiteItem getSiteTree(String url, int depth, String includeByNameRegex, String excludeByNameRegex,
                                Map<String, String> nodeXPathAndExpectedValuePairs) {
        CompositeItemFilter compositeFilter = new CompositeItemFilter();

        if (CollectionUtils.isNotEmpty(defaultFilters)) {
            for (ItemFilter defaultFilter : defaultFilters) {
                compositeFilter.addFilter(defaultFilter);
            }
        }

        if (StringUtils.isNotEmpty(includeByNameRegex)) {
            compositeFilter.addFilter(new IncludeByNameItemFilter(includeByNameRegex));
        }
        if (StringUtils.isNotEmpty(excludeByNameRegex)) {
            compositeFilter.addFilter(new ExcludeByNameItemFilter(excludeByNameRegex));
        }

        if (MapUtils.isNotEmpty(nodeXPathAndExpectedValuePairs)) {
            for (Map.Entry<String, String> pair : nodeXPathAndExpectedValuePairs.entrySet()) {
                compositeFilter.addFilter(new ExpectedNodeValueItemFilter(pair.getKey(), pair.getValue()));
            }
        }

        Tree tree = storeService.findTree(getSiteContext().getContext(), null, url, depth, compositeFilter, null);
        if (tree != null) {
            return createItemWrapper(tree);
        } else {
            return null;
        }
    }

    protected SiteContext getSiteContext() {
        SiteContext siteContext = SiteContext.getCurrent();
        if (siteContext == null) {
            throw new IllegalStateException("No current site context found");
        }

        return siteContext;
    }

    protected SiteItem createItemWrapper(Item item) {
        return new SiteItem(item, modelValueConverters, sortComparator);
    }

}
