package datawave.accumulo.inmemory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AccumuloValidatorsTest {

    // NEW_TABLE_NAME tests

    @Test
    public void testNewTableNameValid() {
        assertDoesNotThrow(() -> AccumuloValidators.NEW_TABLE_NAME.validate("myTable"));
    }

    @Test
    public void testNewTableNameValidWithUnderscore() {
        assertDoesNotThrow(() -> AccumuloValidators.NEW_TABLE_NAME.validate("my_table"));
    }

    @Test
    public void testNewTableNameValidWithDigits() {
        assertDoesNotThrow(() -> AccumuloValidators.NEW_TABLE_NAME.validate("table123"));
    }

    @Test
    public void testNewTableNameValidQualified() {
        assertDoesNotThrow(() -> AccumuloValidators.NEW_TABLE_NAME.validate("namespace.table"));
    }

    @Test
    public void testNewTableNameNull() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate(null));
    }

    @Test
    public void testNewTableNameEmpty() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate(""));
    }

    @Test
    public void testNewTableNameStartsWithDot() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate(".table"));
    }

    @Test
    public void testNewTableNameTooLong() {
        String longName = "a".repeat(1025);
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate(longName));
    }

    @Test
    public void testNewTableNameExactly1024() {
        String name = "a".repeat(1024);
        assertDoesNotThrow(() -> AccumuloValidators.NEW_TABLE_NAME.validate(name));
    }

    @Test
    public void testNewTableNameBlankNamespacePart() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate(".table"));
    }

    @Test
    public void testNewTableNameBlankTablePart() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate("namespace."));
    }

    @Test
    public void testNewTableNameInvalidChars() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate("my-table"));
    }

    @Test
    public void testNewTableNameInvalidCharsInNamespace() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate("name space.table"));
    }

    @Test
    public void testNewTableNameInvalidCharsInTablePart() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate("namespace.my table"));
    }

    // NEW_NAMESPACE_NAME tests

    @Test
    public void testNewNamespaceNameValid() {
        assertDoesNotThrow(() -> AccumuloValidators.NEW_NAMESPACE_NAME.validate("myNamespace"));
    }

    @Test
    public void testNewNamespaceNameValidWithUnderscore() {
        assertDoesNotThrow(() -> AccumuloValidators.NEW_NAMESPACE_NAME.validate("my_namespace"));
    }

    @Test
    public void testNewNamespaceNameNull() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_NAMESPACE_NAME.validate(null));
    }

    @Test
    public void testNewNamespaceNameEmpty() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_NAMESPACE_NAME.validate(""));
    }

    @Test
    public void testNewNamespaceNameTooLong() {
        String longName = "a".repeat(1025);
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_NAMESPACE_NAME.validate(longName));
    }

    @Test
    public void testNewNamespaceNameExactly1024() {
        String name = "a".repeat(1024);
        assertDoesNotThrow(() -> AccumuloValidators.NEW_NAMESPACE_NAME.validate(name));
    }

    @Test
    public void testNewNamespaceNameInvalidChars() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_NAMESPACE_NAME.validate("my-namespace"));
    }

    // EXISTING_NAMESPACE_NAME tests

    @Test
    public void testExistingNamespaceNameValid() {
        assertDoesNotThrow(() -> AccumuloValidators.EXISTING_NAMESPACE_NAME.validate("myNamespace"));
    }

    @Test
    public void testExistingNamespaceNameEmpty() {
        assertDoesNotThrow(() -> AccumuloValidators.EXISTING_NAMESPACE_NAME.validate(""));
    }

    @Test
    public void testExistingNamespaceNameNull() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.EXISTING_NAMESPACE_NAME.validate(null));
    }

    @Test
    public void testExistingNamespaceNameTooLong() {
        String longName = "a".repeat(1025);
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.EXISTING_NAMESPACE_NAME.validate(longName));
    }

    @Test
    public void testExistingNamespaceNameExactly1024() {
        String name = "a".repeat(1024);
        assertDoesNotThrow(() -> AccumuloValidators.EXISTING_NAMESPACE_NAME.validate(name));
    }

    @Test
    public void testExistingNamespaceNameInvalidChars() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.EXISTING_NAMESPACE_NAME.validate("my-namespace"));
    }

    // Validator class tests - verify detailed error messages

    @Test
    public void testValidatorThrowsWithDescriptiveMessageForNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate(null));
        assertTrue(e.getMessage().contains("name is null"), "Expected 'name is null' in: " + e.getMessage());
    }

    @Test
    public void testValidatorThrowsWithDescriptiveMessageForEmpty() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate(""));
        assertTrue(e.getMessage().contains("name is empty"), "Expected 'name is empty' in: " + e.getMessage());
    }

    @Test
    public void testValidatorThrowsWithDescriptiveMessageForDot() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate(".foo"));
        assertTrue(e.getMessage().contains("must not start with a dot"), "Expected dot message in: " + e.getMessage());
    }

    @Test
    public void testValidatorThrowsWithDescriptiveMessageForTooLong() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate("a".repeat(1025)));
        assertTrue(e.getMessage().contains("exceeds maximum length"), "Expected length message in: " + e.getMessage());
    }

    @Test
    public void testValidatorThrowsWithDescriptiveMessageForInvalidChars() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate("my-table"));
        assertTrue(e.getMessage().contains("invalid characters"), "Expected invalid chars message in: " + e.getMessage());
    }

    @Test
    public void testValidatorThrowsWithDescriptiveMessageForBlankTablePart() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.NEW_TABLE_NAME.validate("namespace."));
        assertTrue(e.getMessage().contains("table part must not be blank"), "Expected blank table part message in: " + e.getMessage());
    }

    // EXISTING_TABLE_NAME tests

    @Test
    public void testExistingTableNameValid() {
        assertDoesNotThrow(() -> AccumuloValidators.EXISTING_TABLE_NAME.validate("myTable"));
    }

    @Test
    public void testExistingTableNameValidQualified() {
        assertDoesNotThrow(() -> AccumuloValidators.EXISTING_TABLE_NAME.validate("namespace.table"));
    }

    @Test
    public void testExistingTableNameValidBuiltIn() {
        assertDoesNotThrow(() -> AccumuloValidators.EXISTING_TABLE_NAME.validate("accumulo.metadata"));
    }

    @Test
    public void testExistingTableNameNullInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.EXISTING_TABLE_NAME.validate(null));
    }

    @Test
    public void testExistingTableNameEmptyInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.EXISTING_TABLE_NAME.validate(""));
    }

    @Test
    public void testExistingTableNameLeadingDotInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.EXISTING_TABLE_NAME.validate(".table"));
    }

    @Test
    public void testExistingTableNameBlankTablePartInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.EXISTING_TABLE_NAME.validate("namespace."));
    }

    @Test
    public void testExistingTableNameInvalidChars() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.EXISTING_TABLE_NAME.validate("my-table"));
    }

    @Test
    public void testExistingTableNameTooLong() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.EXISTING_TABLE_NAME.validate("a".repeat(1025)));
    }

    @Test
    public void testExistingTableNameAtMaxLengthValid() {
        assertDoesNotThrow(() -> AccumuloValidators.EXISTING_TABLE_NAME.validate("a".repeat(1024)));
    }

    // qualify tests

    @Test
    public void testQualifyUnqualifiedNameMapsToDefaultNamespace() {
        assertEquals("", AccumuloValidators.qualify("myTable").getLeft());
        assertEquals("myTable", AccumuloValidators.qualify("myTable").getRight());
    }

    @Test
    public void testQualifyQualifiedNameSplitsOnDot() {
        assertEquals("namespace", AccumuloValidators.qualify("namespace.table").getLeft());
        assertEquals("table", AccumuloValidators.qualify("namespace.table").getRight());
    }

    @Test
    public void testQualifyBuiltInTable() {
        assertEquals("accumulo", AccumuloValidators.qualify("accumulo.metadata").getLeft());
        assertEquals("metadata", AccumuloValidators.qualify("accumulo.metadata").getRight());
    }

    @Test
    public void testQualifyValidatesFirst() {
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.qualify(null));
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.qualify(""));
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.qualify(".table"));
        assertThrows(IllegalArgumentException.class, () -> AccumuloValidators.qualify("my-table"));
    }

    // Constructor test

    @Test
    public void testConstructorThrowsUnsupportedOperationException() {
        java.lang.reflect.InvocationTargetException ex = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            java.lang.reflect.Constructor<AccumuloValidators> constructor = AccumuloValidators.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
        assert ex.getCause() instanceof UnsupportedOperationException;
    }
}
