package io.smartdbf.schema;

import java.util.List;

public class DbfSchema {

    private final List<DbfField> fields;

    public DbfSchema(List<DbfField> fields) {
        this.fields = fields;
    }

    public List<DbfField> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (DbfField field : fields) {
            sb.append(field.toString()).append("\n");
        }

        return sb.toString();
    }
}