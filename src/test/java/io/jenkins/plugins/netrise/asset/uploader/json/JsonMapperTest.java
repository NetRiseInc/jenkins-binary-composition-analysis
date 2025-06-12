package io.jenkins.plugins.netrise.asset.uploader.json;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class JsonMapperTest {

    @Test
    public void parseJsonTest() {
        assertThrows(IllegalArgumentException.class, () -> JsonMapper.parseJson(null, Object.class));
        assertThrows(IllegalArgumentException.class, () -> JsonMapper.parseJson("", null));

        assertEquals(
                new TestSimpleObject2(null, null, null, null),
                JsonMapper.parseJson("", TestSimpleObject2.class)
        );
        assertEquals(
                new TestSimpleObject2(null, null, null, null),
                JsonMapper.parseJson("null", TestSimpleObject2.class)
        );
        assertEquals(
                new TestSimpleObject2(null, null, null, null),
                JsonMapper.parseJson("a", TestSimpleObject2.class)
        );
        assertEquals(
                new TestSimpleObject2(null, null, null, null),
                JsonMapper.parseJson("1", TestSimpleObject2.class)
        );
        assertEquals(
                new TestSimpleObject2(null, null, null, null),
                JsonMapper.parseJson("true", TestSimpleObject2.class)
        );
        assertEquals(
                new TestSimpleObject2(null, null, null, null),
                JsonMapper.parseJson("{}", TestSimpleObject2.class)
        );

        assertEquals(
                new TestSimpleObject2("str str", null, null, false),
                JsonMapper.parseJson(
                "{\"s1\":\"str str\",\"bd1\":100500.123,\"bool1\":false}",
                TestSimpleObject2.class)
        );

        // This function is not completed because it parses only the last object in nesting structure.
        assertEquals(new TestSimpleObject2("str str", null, null, false),
                JsonMapper.parseJson("{\"id\":\"id\",\"to\":{\"s1\":\"str str\",\"bool1\":false}}", TestSimpleObject2.class));


        /*assertEquals(new TestChildObject("p1", "p2"),
                JsonMapper.parseJson("{\"prop2\":\"p2\",\"prop1\":\"p1\"}", TestChildObject.class));*/


        assertEquals(new TestObjectWithProps("string 1", "string 2"),
                JsonMapper.parseJson("{\"str1\":\"string 1\",\"s2\":\"string 2\"}", TestObjectWithProps.class));
    }

    @Test
    public void toJsonTest() {
        assertNull(JsonMapper.toJson(null));
        assertEquals("{}", JsonMapper.toJson(new Object()));
        assertEquals("null", JsonMapper.toJson("null"));
        assertEquals("a", JsonMapper.toJson("a"));
        assertEquals("1", JsonMapper.toJson(1));
        assertEquals("true", JsonMapper.toJson(true));

        assertEquals("{\"str1\":\"1\",\"c\":1,\"b\":2,\"s\":3,\"i\":4,\"lo\":5,\"f\":6.6,\"d\":7.7,\"bool1\":true}",
                JsonMapper.toJson(new TestPrimitiveObject("1", '1', (byte) 2, (short) 3, 4, 5L, 6.6f, 7.7, true)));

        assertEquals("{\"s1\":\"str str\",\"int1\":123,\"bd1\":100500.123,\"bool1\":false}",
                JsonMapper.toJson(new TestSimpleObject("str str", 123, new BigDecimal("100500.123"), false)));

        assertEquals("{\"s1\":\"str str\",\"bd1\":100500.123,\"bool1\":false}",
                JsonMapper.toJson(new TestSimpleObject("str str", null, new BigDecimal("100500.123"), false)));


        assertEquals("{\"id\":\"id\",\"to\":{\"s1\":\"str str\",\"bool1\":false}}",
                JsonMapper.toJson(new TestNestedObject("id", new TestSimpleObject("str str", null, null, false))));

        assertEquals("{\"prop2\":\"p2\",\"prop1\":\"p1\"}",
                JsonMapper.toJson(new TestChildObject("p1", "p2")));


        assertEquals("{\"str1\":\"string 1\",\"s2\":\"string 2\"}",
                JsonMapper.toJson(new TestObjectWithProps("string 1", "string 2")));
    }

    record TestPrimitiveObject(String str1, char c,  byte b, short s, int i, long lo, float f, double d, boolean bool1) {}

    record TestSimpleObject(String s1, Integer int1, BigDecimal bd1, Boolean bool1) {}

    record TestSimpleObject2(String s1, Integer int1, Long long1, Boolean bool1) {}

    record TestNestedObject(String id, TestSimpleObject to) {}

    static class TestParentObject {
        private final String prop1;

        public TestParentObject(String prop1) {
            this.prop1 = prop1;
        }

        public String getProp1() {
            return prop1;
        }
    }

    static class TestChildObject extends TestParentObject {
        private final String prop2;

        public TestChildObject(String prop1, String prop2) {
            super(prop1);
            this.prop2 = prop2;
        }

        public String getProp2() {
            return prop2;
        }
    }

    record TestObjectWithProps(@JsonProperty("str1") String s1, String s2) {}


    // DETAILED
    @Test
    public void testParseJson_SimpleObject() {
        String json = "{\"name\":\"Alice\", \"age\":\"25\", \"active\":\"true\"}";
        Map<String, String> result = JsonMapper.parseJson(json);

        assertEquals("Alice", result.get("name"));
        assertEquals("25", result.get("age"));
        assertEquals("true", result.get("active"));
    }

    @Test
    public void testParseJson_EmptyJson() {
        String json = "{}";
        Map<String, String> result = JsonMapper.parseJson(json);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseJson_NonExistentKey() {
        String json = "{\"name\":\"Alice\"}";
        Map<String, String> result = JsonMapper.parseJson(json);

        assertNull(result.get("age")); // Key does not exist
    }

    static class Person {
        private final String name;
        private final int age;
        private final boolean active;

        public Person(String name, int age, boolean active) {
            this.name = name;
            this.age = age;
            this.active = active;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isActive() { return active; }
    }

    @Test
    public void testParseJsonToObject() {
        String json = "{\"name\":\"Alice\", \"age\":\"25\", \"active\":\"true\"}";
        Person person = JsonMapper.parseJson(json, Person.class);

        assertNotNull(person);
        assertEquals("Alice", person.getName());
        assertEquals(25, person.getAge());
        assertTrue(person.isActive());
    }

    @Test
    public void testParseJsonToObject_NullJson() {
        assertThrows(IllegalArgumentException.class, () -> JsonMapper.parseJson(null, Person.class));
    }

    @Test
    public void testParseJsonToObject_NullClass() {
        String json = "{\"name\":\"Alice\", \"age\":\"25\"}";
        assertThrows(IllegalArgumentException.class, () -> JsonMapper.parseJson(json, null));
    }

    static class Car {
        private final String brand;
        private final int year;

        public Car(String brand, int year) {
            this.brand = brand;
            this.year = year;
        }

        public String getBrand() { return brand; }
        public int getYear() { return year; }
    }

    @Test
    public void testToJson_SimpleObject() {
        Car car = new Car("Tesla", 2023);
        String json = JsonMapper.toJson(car);

        assertTrue(json.contains("\"brand\":\"Tesla\""));
        assertTrue(json.contains("\"year\":2023"));
    }

    @Test
    public void testToJson_NullObject() {
        String json = JsonMapper.toJson(null);
        assertNull(json);
    }

    static class Address {
        private final String city;
        private final String country;

        public Address(String city, String country) {
            this.city = city;
            this.country = country;
        }

        public String getCity() { return city; }
        public String getCountry() { return country; }
    }

    static class User {
        private final String name;
        private final Address address;

        public User(String name, Address address) {
            this.name = name;
            this.address = address;
        }

        public String getName() { return name; }
        public Address getAddress() { return address; }
    }

    /*@Test
    public void testParseJson_NestedObject() {
        String json = "{\"name\":\"Alice\", \"address\":{\"city\":\"Capital\", \"country\":\"Ooz\"}}";
        User user = JsonMapper.parseJson(json, User.class);

        assertNotNull(user);
        assertEquals("Alice", user.getName());
        assertEquals("Riga", user.getAddress().getCity());
        assertEquals("Latvia", user.getAddress().getCountry());
    }

    static class SkillSet {
        private final List<String> skills;

        public SkillSet(List<String> skills) {
            this.skills = skills;
        }

        public List<String> getSkills() { return skills; }
    }

    @Test
    void testParseJson_Array() {
        String json = "{\"skills\":[\"Java\", \"Python\", \"C++\"]}";
        SkillSet skillSet = JsonMapper.parseJson(json, SkillSet.class);

        assertNotNull(skillSet);
        assertEquals(3, skillSet.getSkills().size());
        assertTrue(skillSet.getSkills().contains("Java"));
        assertTrue(skillSet.getSkills().contains("Python"));
        assertTrue(skillSet.getSkills().contains("C++"));
    }

    @Test
    void testParseJson_EmptyArray() {
        String json = "{\"skills\":[]}";
        SkillSet skillSet = JsonMapper.parseJson(json, SkillSet.class);

        assertNotNull(skillSet);
        assertTrue(skillSet.getSkills().isEmpty());
    }
    */

    @Test
    public void testToJson_NestedObject() {
        Address address = new Address("Berlin", "Germany");
        User user = new User("Alice", address);

        String json = JsonMapper.toJson(user);

        assertTrue(json.contains("\"name\":\"Alice\""));
        assertTrue(json.contains("\"address\":{\"city\":\"Berlin\",\"country\":\"Germany\"}"));
    }

    @Test
    public void testToJson_NullNestedObject() {
        User user = new User("Bob", null);

        String json = JsonMapper.toJson(user);

        assertTrue(json.contains("\"name\":\"Bob\""));
        assertFalse(json.contains("\"address\":")); // Address should be omitted
    }

    static class SkilledUser {
        private final String name;
        private final Address address;
        private final List<String> skills;

        public SkilledUser(String name, Address address, List<String> skills) {
            this.name = name;
            this.address = address;
            this.skills = skills;
        }

        public String getName() { return name; }
        public Address getAddress() { return address; }
        public List<String> getSkills() { return skills; }
    }

    @Test
    public void testToJson_DeepNestedObjectWithArray() {
        Address address = new Address("Paris", "France");
        SkilledUser user = new SkilledUser("Alice", address, List.of("Java", "Python", "C++"));

        String json = JsonMapper.toJson(user);

        assertTrue(json.contains("\"name\":\"Alice\""));
        assertTrue(json.contains("\"address\":{\"city\":\"Paris\",\"country\":\"France\"}"));
        assertTrue(json.contains("\"skills\":[\"Java\",\"Python\",\"C++\"]"));
    }

    @Test
    public void testToJson_NullNestedObjectWithArray() {
        SkilledUser user = new SkilledUser("Bob", null, List.of("JavaScript", "Go"));

        String json = JsonMapper.toJson(user);

        assertTrue(json.contains("\"name\":\"Bob\""));
        assertFalse(json.contains("\"address\":")); // Address should be omitted
        assertTrue(json.contains("\"skills\":[\"JavaScript\",\"Go\"]"));
    }

    @Test
    public void testToJson_EmptyArrayInNestedObject() {
        Address address = new Address("Berlin", "Germany");
        SkilledUser user = new SkilledUser("Charlie", address, List.of());

        String json = JsonMapper.toJson(user);

        assertTrue(json.contains("\"name\":\"Charlie\""));
        assertTrue(json.contains("\"address\":{\"city\":\"Berlin\",\"country\":\"Germany\"}"));
        assertTrue(json.contains("\"skills\":[]")); // Empty array should be correctly serialized
    }


}
