package graphql.schema;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class PropertyDataFetcher implements DataFetcher {

    private final String propertyName;

    public PropertyDataFetcher(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        Object source = environment.getSource();
        if (source == null) return null;
        if (source instanceof Map) {
            return ((Map<?, ?>) source).get(propertyName);
        }
        return getPropertyViaGetter(source);
    }

    private Object getPropertyViaGetter(Object object) {
        String getterName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        try {
            Method method = object.getClass().getMethod(getterName);
            return method.invoke(object);

        } catch (NoSuchMethodException e) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
