package io.github.vikaschaudharyarit_max.smartdbf.mapper;

import io.github.vikaschaudharyarit_max.smartdbf.annotation.DbfColumn;
import io.github.vikaschaudharyarit_max.smartdbf.core.DbfReader;
import io.github.vikaschaudharyarit_max.smartdbf.exception.DbfException;
import io.github.vikaschaudharyarit_max.smartdbf.schema.DbfField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps raw {@code Object[]} rows produced by {@link DbfReader} to instances of a target POJO class.
 *
 * <p>Field binding is resolved at construction time via reflection. Each Java field on the target
 * class is matched to a DBF column either by the {@link DbfColumn} annotation value or, as a
 * fallback, by the Java field name (case-insensitive).
 *
 * <p>Type coercion is applied automatically:
 * <ul>
 *   <li>{@link BigDecimal} → {@link Double}, {@link Float}, {@link Long}, {@link Integer},
 *       {@link Short}, {@link java.math.BigDecimal}</li>
 *   <li>{@link LocalDate} → {@link java.util.Date}, {@link java.sql.Date}, {@link LocalDate}</li>
 *   <li>{@link String} → {@link String} (or any target type via {@code toString()})</li>
 * </ul>
 *
 * @param <T> the target POJO type
 */
public class DbfMapper<T> {

    private static final Logger log = LoggerFactory.getLogger(DbfMapper.class);

    private final Class<T> type;
    private final List<FieldBinding> bindings = new ArrayList<>();

    public DbfMapper(Class<T> type, DbfReader reader) {
        this.type = type;

        List<DbfField> dbfFields = reader.getSchema().getFields();

        Map<String, Integer> columnIndexMap = new HashMap<>();
        for (int i = 0; i < dbfFields.size(); i++) {
            columnIndexMap.put(dbfFields.get(i).getName().toLowerCase(), i);
        }

        for (Field classField : type.getDeclaredFields()) {
            String dbfColumnName = classField.isAnnotationPresent(DbfColumn.class)
                    ? classField.getAnnotation(DbfColumn.class).value()
                    : classField.getName();

            Integer columnIndex = columnIndexMap.get(dbfColumnName.toLowerCase());
            if (columnIndex != null) {
                bindings.add(new FieldBinding(columnIndex, classField));
                log.debug("Bound field '{}' -> DBF column '{}' (index {})",
                        classField.getName(), dbfColumnName, columnIndex);
            }
        }

        log.debug("DbfMapper for '{}': {} of {} class fields bound",
                type.getSimpleName(), bindings.size(), type.getDeclaredFields().length);
    }

    /**
     * Maps a single raw record row to an instance of {@code T}.
     *
     * @param row the raw values returned by {@link DbfReader#nextRecord()}
     * @return a populated instance of {@code T}
     * @throws DbfException if the instance cannot be created or a field cannot be set
     */
    public T map(Object[] row) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            T instance = ctor.newInstance();

            for (FieldBinding binding : bindings) {
                Object value = row[binding.getColumnIndex()];
                if (value == null) {
                    continue;
                }
                Object coerced = coerce(value, binding.getField().getType(), binding.getField().getName());
                binding.getField().set(instance, coerced);
            }

            return instance;
        } catch (DbfException e) {
            throw e;
        } catch (Exception e) {
            throw new DbfException("Failed to map row to " + type.getSimpleName(), e);
        }
    }

    /**
     * Converts {@code value} (as produced by {@link io.github.vikaschaudharyarit_max.smartdbf.core.DbfReader})
     * to the Java type declared on the target field.
     *
     * <p>DBF field types produce these source types:
     * <ul>
     *   <li>C → String</li>
     *   <li>N/F → BigDecimal</li>
     *   <li>D → LocalDate</li>
     *   <li>L → Boolean</li>
     * </ul>
     */
    private Object coerce(Object value, Class<?> targetType, String fieldName) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }

        try {
            // --- BigDecimal (from N/F fields) → numeric Java types ---
            if (value instanceof BigDecimal bd) {
                if (targetType == Double.class   || targetType == double.class)   return bd.doubleValue();
                if (targetType == Float.class    || targetType == float.class)    return bd.floatValue();
                if (targetType == Long.class     || targetType == long.class)     return bd.longValue();
                if (targetType == Integer.class  || targetType == int.class)      return bd.intValue();
                if (targetType == Short.class    || targetType == short.class)    return bd.shortValue();
                if (targetType == BigDecimal.class)                               return bd;
                if (targetType == String.class)                                   return bd.toPlainString();
            }

            // --- LocalDate (from D fields) → date types ---
            if (value instanceof LocalDate ld) {
                if (targetType == java.sql.Date.class)  return java.sql.Date.valueOf(ld);
                if (targetType == java.util.Date.class) return java.sql.Date.valueOf(ld);
                if (targetType == String.class)         return ld.toString();
                if (targetType == LocalDate.class)      return ld;
            }

            // --- String → other types (fallback) ---
            if (value instanceof String s) {
                if (targetType == Long.class    || targetType == long.class)    return Long.parseLong(s.trim());
                if (targetType == Integer.class || targetType == int.class)     return Integer.parseInt(s.trim());
                if (targetType == Double.class  || targetType == double.class)  return Double.parseDouble(s.trim());
                if (targetType == BigDecimal.class)                             return new BigDecimal(s.trim());
                if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(s.trim());
            }

            // --- Boolean → String ---
            if (value instanceof Boolean b && targetType == String.class) {
                return b.toString();
            }

        } catch (Exception e) {
            log.warn("Type coercion failed for field '{}': cannot convert {} to {} — returning null",
                    fieldName, value.getClass().getSimpleName(), targetType.getSimpleName());
            return null;
        }

        log.warn("No coercion path from {} to {} for field '{}' — returning raw value",
                value.getClass().getSimpleName(), targetType.getSimpleName(), fieldName);
        return value;
    }
}
