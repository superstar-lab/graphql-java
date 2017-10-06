package graphql.schema;


import graphql.GraphQLException;
import graphql.PublicApi;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import static graphql.Scalars.GraphQLBoolean;

/**
 * This is the default data fetcher used in graphql-java.  It will examine
 * maps and POJO java beans for values that match the desired name, typically the field name.
 *
 * You can write your own data fetchers to get data from some other backing system
 *
 * @see graphql.schema.DataFetcher
 */
@PublicApi
public class PropertyDataFetcher<T> implements DataFetcher<T> {

    private final String propertyName;

    public PropertyDataFetcher(String propertyName) {
        this.propertyName = propertyName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(DataFetchingEnvironment environment) {
        Object source = environment.getSource();
        if (source == null) return null;
        if (source instanceof Map) {
            return (T) ((Map<?, ?>) source).get(propertyName);
        }
        return (T) getPropertyViaGetter(source, environment.getFieldType());
    }

    /**
     * Invoking public methods on package-protected classes via reflection
     * causes exceptions. This method searches a class's hierarchy for
     * public visibility parent classes with the desired getter. This
     * particular case is required to support AutoValue style data classes,
     * which have abstract public interfaces implemented by package-protected
     * (generated) subclasses.
     */
    private Method findAccessibleMethod(Class root, String methodName) throws NoSuchMethodException {
        Class cur = root;
        while (cur != null) {
            if (Modifier.isPublic(cur.getModifiers())) {
                @SuppressWarnings("unchecked")
                Method m = cur.getMethod(methodName);
                if (Modifier.isPublic(m.getModifiers())) {
                    return m;
                }
            }
            cur = cur.getSuperclass();
        }
        //noinspection unchecked
        return root.getMethod(methodName);
    }

    private Object getPropertyViaGetter(Object object, GraphQLOutputType outputType) {
        try {
            if (isBooleanProperty(outputType)) {
                try {
                    return getPropertyViaGetterUsingPrefix(object, "is");
                } catch (NoSuchMethodException e) {
                    return getPropertyViaGetterUsingPrefix(object, "get");
                }
            } else {
                return getPropertyViaGetterUsingPrefix(object, "get");
            }
        } catch (NoSuchMethodException e1) {
            return getPropertyViaFieldAccess(object);
        }
    }

    private Object getPropertyViaGetterUsingPrefix(Object object, String prefix) throws NoSuchMethodException {
        String getterName = prefix + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        try {
            Method method = findAccessibleMethod(object.getClass(), getterName);
            return method.invoke(object);

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new GraphQLException(e);
        }
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isBooleanProperty(GraphQLOutputType outputType) {
        if (outputType == GraphQLBoolean) return true;
        if (outputType instanceof GraphQLNonNull) {
            return ((GraphQLNonNull) outputType).getWrappedType() == GraphQLBoolean;
        }
        return false;
    }

    private Object getPropertyViaFieldAccess(Object object) {
        try {
            Field field = object.getClass().getField(propertyName);
            return field.get(object);
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException e) {
            throw new GraphQLException(e);
        }
    }
}
