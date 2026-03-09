package io.github.vikaschaudharyarit_max.smartdbf.mapper;

import io.github.vikaschaudharyarit_max.smartdbf.core.DbfReader;
import io.github.vikaschaudharyarit_max.smartdbf.schema.DbfField;
import io.github.vikaschaudharyarit_max.smartdbf.annotation.DbfColumn;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.*;

public class DbfMapper<T> {

    private final Class<T> type;
    private final List<FieldBinding> bindings = new ArrayList<>();

    public DbfMapper(Class<T> type, DbfReader reader) {

        this.type = type;
    
        List<DbfField> dbfFields = reader.getSchema().getFields();
    
        Map<String, Integer> columnIndexMap = new HashMap<>();
    
        for (int i = 0; i < dbfFields.size(); i++) {
            columnIndexMap.put(
                dbfFields.get(i).getName().toLowerCase(),
                i
            );
        }
    
        Field[] classFields = type.getDeclaredFields();
    
        for (Field classField : classFields) {
    
            String dbfFieldName = classField.getName();
    
            if (classField.isAnnotationPresent(DbfColumn.class)) {
                dbfFieldName = classField
                        .getAnnotation(DbfColumn.class)
                        .value();
            }
    
            Integer columnIndex =
                    columnIndexMap.get(dbfFieldName.toLowerCase());
    
            if (columnIndex != null) {
    
                bindings.add(new FieldBinding(columnIndex, classField));
    
            }
        }
    }

    public T map(Object[] row) {

        try {

            T instance = type.getDeclaredConstructor().newInstance();

            for (FieldBinding binding : bindings) {

                Object value = row[binding.getColumnIndex()];

                binding.getField().set(instance, value);
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
