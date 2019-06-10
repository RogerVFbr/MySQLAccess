# MySQL Access
The MySQLAccess module is largely inspired by no-sql database access 
libraries used in front-end Javascript projects, such as AngularFire 
and VueFire. It's built as a form of wrapper around the native Java JDBC
framework, with the intention of creating an even more abstract and simplified
way of interacting with MySQL databases and handling the decoding and encoding of data sent and received from it. 
The concept behind this module is that the fields in the model will dictate what columns will be retrieved from
the database. The algorithm will do it's best to figure out by himself what
columns are to be directed to each field, based on field and column name similarity
and data type as well.

## Pre-requisites
- Needs at least Java SDK 12.
- To use local database install MySQL Community Server (https://dev.mysql.com/downloads/mysql/)
- To manage your databases visually use MySQL Workbench (https://dev.mysql.com/downloads/workbench/)
- Information you need to know before you use this:
  - Target server I.P.
  - Target server port number
  - Database name
  - Username
  - Password
  - Names of the tables you need to access
  - Names and types of the columns of these tables

## SQL Connector Installation
The SQL Connector is needed on any Java project using JDBC and dealing with MySQL 
databases. You will need to install it on your project before using MySQLAccess, since
it relies on Java JDBC.
- Download JDBC connector at https://dev.mysql.com/downloads/connector/j/
- Create LIB folder in project folder.
- Copy JAR file from downloaded zip to LIB folder (Ex. mysql-connector-java-8.0.16.jar)
- The steps for including the connector on your project will depend on your IDE of choice.
- If you use Eclipse, find the instruction on this link: http://www.ccs.neu.edu/home/kathleen/classes/cs3200/JDBCtutorial.pdf
- If you use IntelliJ, follow the steps below:
  - Go to menu FILE > PROJECT STRUCTURE > LIBRARIES
  - Click on plus sign
  - Select Java
  - Navigate to JAR file and apply
- For other IDEs, please search the web for instructions.

## Installation
Just copy the MySQLAccess class to your project and import accordingly.

## Usage
This section will explain how each one of the public methods contained in 
this module behave and how they should be used.

### Step 0: Create a model for table interaction
The first step in using this module is creating a model, a class containing fields that are equivalent
in name and type to the target table's columns:
```
public class Employee {

    // Attributes names must match database table column names.
    private int id;
    private String name;
    private String dept;
    private int salary;

    // For use with MySQLAccess, model has to implement parameterless constructor
    public Employee () {}
    
    @Override
    public String toString() {
        return "Employee { " +
                "id=" + id +
                ", name='" + name + '\'' +
                ", dept='" + dept + '\'' +
                ", salary=" + salary + " " +
                '}';
    }
}
```
MySQLAccess will use the model's field names to automatically map any data incoming or outgoing from/to the database.
For instance, a table containing columns with names ID, NAME, DEPT and SALARY would have it's data
routed directly to a model as long as it's field names are similar and data types are compatible.
The example above shows the minimum possible implementation of a model capable of sending/receiving
data from MySQLAccess methods. 

It's essential to implement a constructor without parameters, however, you can
implement as many constructors as you want. You can also implement other methods, such as getters/setters, 
they will not impact the algorithm. 'toString' method is not needed, but advisable. Make sure to name your model fields 
with the same names of your table columns. Similar names will also work, however, exact names are recommended. It's 
also possible to omit some of the fields and retrieve only the required columns.

```
public class EmployeeNameAndId {

    // Attributes names must match database table column names.
    private int id;
    private String name;

    // For use with MySQLAccess, model has to implement parameterless constructor
    public EmployeeNameAndId () {}
    
    @Override
    public String toString() {
        return "EmployeeNameAndId { " +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
```

The example above will only retrieve the columns with best name and type match with the fields 'id' and 'name'.

### Step 1: MySQLAccess.Config - Creating a configuration object
Though different constructors are available for the instantiation of the MySQLAccess module, it's recommended to use 
the provided configuration object. At this point you will need the information for accessing you MySQL database.
The sequence of parameters is: IP, port number, database name, user name and password.
```
public static final MySQLAccess.Config LOCAL = new MySQLAccess.Config(
        "127.0.0.1",
        3306,
        "test_database",
        "root",
        "rootpass"
);
```
This object should typically be stored in a different file, one that you can add to .gitignore and avoid sensitive data
leakage.

### Step 2: Instantiate MySQLAccess object on your project
You are now ready to instantiate MySQLAccess object on the desired sector of your project. Do this by using the 
configuration object you created on step 1.
```
MySQLAccess database = new MySQLAccess(DatabaseConfig.LOCAL);
```
It's also possible to pass the default table name to the constructor, and use different instances to access different
tables. 
```
MySQLAccess employeesDb = new MySQLAccess(DatabaseConfig.LOCAL, "employees_tbl");
MySQLAccess songsDb = new MySQLAccess(DatabaseConfig.LOCAL, "songs_tbl");
```
Although about 80% of the module's content lies on static methods and multiple instances should not weight too much
on the system, it's advisable to keep module instantiation on project at a minimum and change tables dynamically as will
be shown on next steps.

If you prefer not to use the configuration object provided, you can also use the inline instantiation, including a 
default table name or not:
```
MySQLAccess database = new MySQLAccess("127.0.0.1", 3306, "test_database", "root", "rootpass");
MySQLAccess employeesDb = new MySQLAccess("127.0.0.1", 3306, "test_database", "root", "rootpass", "employees_tbl");
```

### Step 3: Activate native logging if needed
MySQLAccess, comes bundled with a native logging system, design to show to the developer the inner workings of the 
module. Logging is activated system-wide and should be done right after module instantiation:
```
database.logDetails();
database.logInfo();
database.logFetch();
```
These loggers show the following information:
- logDetails: shows details about the data incoming and outgoing from and to the database.
- logInfo: show the actions that the algorithm is taking to communicate with the database.
- logFetch: shows the information the module is gathering about the database and table properties.