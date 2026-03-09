import io.github.vikaschaudharyarit_max.smartdbf.core.Dbf;
import io.github.vikaschaudharyarit_max.smartdbf.core.DbfReader;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class DbfTest {

    @Test
    void testOpenDbf() {

        DbfReader reader = Dbf.open("C:\\Users\\vikaschaudhary\\Downloads\\cams_jan1.dbf");

        while(reader.hasNext()) {
            Object[] record = reader.nextRecord();

            // System.out.println(Arrays.toString(record));
        }
        System.out.println("Done");
        System.out.println(reader.getSchema().toString());
    }
}
