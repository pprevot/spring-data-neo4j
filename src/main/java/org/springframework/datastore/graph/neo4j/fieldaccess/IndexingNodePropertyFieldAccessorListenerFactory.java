package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.index.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.api.GraphEntity;
import org.springframework.datastore.graph.api.GraphEntityProperty;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.ReflectUtils;

import java.lang.reflect.Field;

import static org.springframework.datastore.graph.neo4j.support.ReflectUtils.getAnnotation;

@Configurable
class IndexingNodePropertyFieldAccessorListenerFactory implements FieldAccessorListenerFactory<NodeBacked> {
    @Autowired
    private IndexService indexService;
    private final PropertyFieldAccessorFactory propertyFieldAccessorFactory;
    private final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory;

    IndexingNodePropertyFieldAccessorListenerFactory(final PropertyFieldAccessorFactory propertyFieldAccessorFactory, final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory) {
        this.propertyFieldAccessorFactory = propertyFieldAccessorFactory;
        this.convertingNodePropertyFieldAccessorFactory = convertingNodePropertyFieldAccessorFactory;
    }

    @Override
    public boolean accept(final Field f) {
        return isPropertyField(f) && isIndexed(f);
    }

    private boolean isIndexed(final Field f) {
        final GraphEntity entityAnnotation = getAnnotation(f.getDeclaringClass(), GraphEntity.class);
        if (entityAnnotation!=null && entityAnnotation.fullIndex()) return true;
        final GraphEntityProperty propertyAnnotation = getAnnotation(f, GraphEntityProperty.class);
        return propertyAnnotation!=null && propertyAnnotation.index();
    }

    private boolean isPropertyField(final Field f) {
        return propertyFieldAccessorFactory.accept(f) || convertingNodePropertyFieldAccessorFactory.accept(f);
    }

    @Override
    public FieldAccessListener<NodeBacked,?> forField(final Field field) {
        return new IndexingNodePropertyFieldAccessorListener(field,indexService);
    }

	/**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class IndexingNodePropertyFieldAccessorListener implements FieldAccessListener<NodeBacked, Object> {
	    protected final Field field;
	    private final IndexService indexService;

	    public IndexingNodePropertyFieldAccessorListener(final Field field, final IndexService indexService) {
	        this.field = field;
	        this.indexService = indexService;
	    }

	    @Override
	    public void valueChanged(final NodeBacked nodeBacked, final Object oldVal, final Object newVal) {
	        if (newVal==null) indexService.removeIndex(nodeBacked.getUnderlyingState(),getPropertyName());
	        else indexService.index(nodeBacked.getUnderlyingState(), getPropertyName(),newVal);
	    }

	    // todo
	    private String getPropertyName() {
	        return DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
	    }

	}
}