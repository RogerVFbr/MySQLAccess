# MySQL Access
The MySQLAccess module is largely inspired by no-sql database access 
libraries used in front-end Javascript projects, such as AngularFire 
and VueFire. It's built as a form of wrapper around the native Java JDBC
framework, with the intention of creating an even more abstract and simplified
way of accessing MySQL databases. The concept behind this module is the idea 
that the fields in the model will dictate what columns will be retrieved from
the database. The algorithm will do it's best to figure out by himself what
columns are to be directed to each field, based on field and column name similarity
and data type as well.

## Pre-requisites
- Needs at least Java SDK 12.
- To use local database install MySQL Community Server (https://dev.mysql.com/downloads/mysql/)
- To manage your databases visually use MySQL Workbench (https://dev.mysql.com/downloads/workbench/)

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

### Model / Table Correlation
This concept is essential for the comprehension of the inner workings of this algorithm.
MySQLAccess will use the model's field names to automatically map any data incoming or outgoing from/to the database.
For instance, a table containing columns with names ID, NAME, DEPT and SALARY would have it's data
routed directly to a model as long as it's field names are similar and data types are compatible.
The example below shows the minimum possible implementation of a model capable of sending/receiving
data from MySQLAccess methods. It's essential to implement a constructor without parameters, however, you can
implement as many constructors as you want. You can also implement other methods, such as getters/setters or
toString, they will not impact the algorithm.

```
public class Employee {

    // Attributes names must match database table column names.
    private int id;
    private String name;
    private String dept;
    private int salary;

    // For use with MySQLAccess, model has to implement parameterless constructor
    public Employee () {}
}
```

Make sure to name your model fields with the same names of your table columns. Similar names will also work, 
however, exact names are recommended. It's also possible to omit some of the fields and retrieve only the required
columns.

```
public class EmployeeNameAndId {

    // Attributes names must match database table column names.
    private int id;
    private String name;

    // For use with MySQLAccess, model has to implement parameterless constructor
    public Employee () {}
}
```

The example above will only retrieve the columns with best name and type match with the fields id and name.
