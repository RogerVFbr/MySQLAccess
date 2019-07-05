# MySQL Access
The **MySQLAccess** module is a personal attempt to create a largely simplified, compact and lightweight **Java O.R.M. 
tool** specialized in MySQL, inspired by algorithms like *Hibernate*, *iBatis* or *Toplink*. **O.R.M.** stands 
for object-relational mapping, and refers to implementations that aim to make the exchange of data between the memory of 
applications (usually in the form of lists or individual variables or objects) and persistent SQL based databases 
(tables and rows) much easier and streamlined than the standard methodology done via Java JDBC, SQL queries and the 
subsequent decoding/encoding of the data received/sent. 

If you don't know what an O.R.M. tool is or have never used one, you can find more info about the subject and popular 
technologies on the field at the following links:

- http://www.providenceconsulting.in/technologies/java-jee/java-orm-tools.html
- https://en.wikipedia.org/wiki/List_of_object-relational_mapping_software    

The concept behind this module is that the fields in the model will dictate which table columns will be retrieved from
or manipulated on the database. The algorithm will do it's best to figure out by himself what
columns are to be directed to each field, based on name similarity
and data type compatibility.

## Table of contents
* [Pre-requisites](#pre-requisites)
* [MySQL Connector Installation](#mysql-connector-installation)
* [Installation](#installation)
* [Setting Up](#setting-up)
* [Database operations](#database-operations-crud)
* [Extra operations](#extra-operations)
* [Logging](#logging)


## Pre-requisites
- Requires Java SDK 12.
- To use local database install MySQL Community Server (https://dev.mysql.com/downloads/mysql/)
- To manage your databases visually use MySQL Workbench (https://dev.mysql.com/downloads/workbench/)
- Information about the database you wish to manipulate (server I.P., port number, database name, username, password, 
names of the tables you need to access, names and types of the columns on these tables)

## MySQL Connector Installation
The MySQL Connector is needed on any Java project using JDBC and dealing with MySQL 
databases. You will need to install it on your project before using MySQLAccess, since
it relies on Java JDBC.
- Download JDBC connector at https://dev.mysql.com/downloads/connector/j/
- Create LIB folder in project folder.
- Copy JAR file from downloaded zip to LIB folder (Ex. mysql-connector-java-8.0.16.jar)
- The steps for including the connector on your project class path will depend on your IDE of choice.
- If you use **Eclipse**, find the instructions on this link: http://www.ccs.neu.edu/home/kathleen/classes/cs3200/JDBCtutorial.pdf
- If you use **Netbeans**, find instructions here: https://stackoverflow.com/questions/24490361/java-lang-classnotfoundexceptioncom-mysql-jdbc-driver-in-netbeans
- If you use **IntelliJ**, follow the steps below:
  - Go to menu FILE > PROJECT STRUCTURE > LIBRARIES
  - Click on plus sign
  - Select Java
  - Navigate to JAR file and apply
- For other IDEs, please search the web for instructions.

## Installation
Just copy the 'mysqlaccess' package to your project and import 'MySQLAccess' class.

## Setting up
This section will explain each of the steps to instantiate and get started with using the module.

### Step 0: Create a model for table interaction
The first step in using this module is creating a model, a class containing fields that are equivalent
in name and type to the target table's columns. Let's suppose you are trying to manipulate the values of the
following table:
```    
                                     EMPLOYEE'S TABLE ('employees_tbl')          
                        +----------------------------------------------------------+
                        |  id   |        name        |     dept      |    salary   |
                        +----------------------------------------------------------+ 
                        |   1       Marcus Garvey       Sales            2000      | 
                        |   2       Celina Gomes        IT               3000      | 
                        |   3       Rosetta Stone       Deliveries       2500      | 
                        |   4       Robson Charles      IT               4000      | 
                        +----------------------------------------------------------+  
```
Create a model as such:
```
public class Employee {

    // Field names must match database table column names.
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
The example above shows the minimum possible implementation of a model capable of sending/receiving
data from MySQLAccess methods. 

It's essential to implement a constructor without parameters, however, you can
implement as many constructors as you want. You can also implement other methods, such as getters/setters, 
they will not impact the algorithm. 'toString' method is not needed, but advisable. Make sure to name your model fields 
with the same names of your table columns. Similar names will also work (i.e upper/lowercase, plurals), however, 
exact names are recommended. It's also possible to omit some of the fields. This will make the algorithm work only on 
the required columns.

```
public class EmployeeNameAndId {

    // Field names must match database table column names.
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

### Step 1: creating a MySQLAConfig configuration object
Though different constructors are available for the instantiation of the MySQLAccess module, it's recommended to use 
the provided configuration object structure. At this point you will need the information for accessing your MySQL database.
The sequence of parameters is: IP, port number, database name, user name and password.
```
MySQLAConfig LOCAL = new MySQLAConfig(
        "127.0.0.1",            // ---> Server I.P
        3306,                   // ---> Server port
        "test_database",        // ---> Database name
        "root",                 // ---> User name
        "rootpass"              // ---> Password
);
```
This object should typically be stored in a different file, one that you can add to .gitignore and avoid sensitive data
leakage.

### Step 2: Instantiate MySQLAccess object on your project
You are now ready to instantiate MySQLAccess object on the desired sector of your project. Do this by passing to the 
constructor the configuration object you created on step 1.
```
MySQLAccess database = new MySQLAccess(LOCAL);
```
It's also possible to pass the default table name to the constructor, and use different instances to access different
tables. 
```
MySQLAccess employeesTbl = new MySQLAccess(LOCAL, "employees_tbl");
MySQLAccess songsTbl = new MySQLAccess(LOCAL, "songs_tbl");
```
Although about 80% of the module's content lies on static methods and multiple instances should not weight too much
on the system, it's advisable to keep module instantiation on project at a minimum and change tables dynamically as will
be shown on next steps.

If you prefer not to use the configuration object provided, you can also use the inline instantiation, including a 
default table name or not as the last parameter on the constructor:
```
MySQLAccess database = new MySQLAccess("127.0.0.1", 3306, "test_database", "root", "rootpass");
MySQLAccess employeesTbl = new MySQLAccess("127.0.0.1", 3306, "test_database", "root", "rootpass", "employees_tbl");
```

### Step 3: Set target table
MySQLAccess instances can have their target table changed dynamically. If during instantiation no default table was
passed as an argument, this step is mandatory:
```
database.setTable("employees_tbl");
..... perform operations on employees table .....

database.setTable("[PASS A STRING CONTAINING THE TARGET TABLE'S NAME]");
..... perform operations on your table .....
```
It's useful to work with only one instance of the module and change tables according to your needs,
if you wish to keep memory usage low.

## Database operations (C.R.U.D.)
This section will explain how to perform each of the basic C.R.U.D (create, read, update and delete) operations on 
the database. Examples expect that the MySQLAccess object has been instantiated on the 'database' variable and that
a proper table has been selected either on the constructor or via the 'setTable' method previously explained.

### Get (Read)
To retrieve data from the selected table directly into a list of the desired type use the following command:
```
List<Employee> employees = database.get(Employee.class);
```
For models with a subset of the original model fields, only necessary columns will be fetched.
```
List<EmployeeNameAndId> employeeNameAndIds = database.get(EmployeeNameAndId.class);
```
For models with more fields than available columns on table, only available will be mapped.

First overload sends SQL WHERE clause using column name to filter. I.e.: "salary > 4000", "id = 100".
```
Employee employeeMichael = database.get(Employee.class, "name = 'Michael'").get(0);
List<EmployeeNameAndId> employeeNamesAndIdsFromSalesDept = 
           database.get(EmployeeNameAndId.class, "dept = 'Sales'");
```
Add OnGetComplete object as last parameter on any overload to achieve asynchronous execution.
```
database.get(Employee.class, new OnGetComplete<Employee>() {
    @Override
    public void onSuccess(List<Employee> data) {
    
    }
    
    @Override
    public void onFailure() {
    
    }
});
```

### Add (Create)
Add new row to table from a new model instance (field names must match table columns as much as possible and types 
must be compatible). Fields pointing to columns that automatically generate their values on the table will simply 
be ignored.
```
Employee newEmployee = new Employee(
        1,
        "Simon Fuller",
        "IT",
        4500
);

database.add(newEmployee);
```
If you wish to test whether the command has been successfully executed, assign method return to an 'Object' variable. 
A null result means the operation has NOT been successfully completed. If operation succeeds, the key for the newly 
created row will be retrieved.
```
Object result = database.add(newEmployee);

if (result != null) {
    System.out.println("Successfully added new row under following primary key: " + result);
}
else {
    System.out.println("Could not add new row to table.");
}
```
To achieve asynchronous operation and react upon completion, use callbacks:
```
database.add(newEmployee, OnComplete<String>() {
    @Override
    public void onSuccess(String feedback) {

    }

    @Override
    public void onFailure() {

    }
});
```
'feedback' String parameter on the onSuccess callback will return the key generated for the newly created row on the
table.

### Update (Update)
Use this to modify a row on a table. The model must have a primary key equivalent field in type and name. 
The algorithm will use the field corresponding to the primary key to figure out which row is to be updated.
Note that the entire row will be overwritten by the new content.
```
Employee updatedEmployee = new Employee(
        30,
        "Rosie Miller",
        "Sales",
        3000
);

database.update(updatedEmployee);
```
If you wish to test whether the command has been successfully executed, assign method return to an 'Integer' variable. 
A null result means the operation has NOT been successfully completed. If operation succeeds, the number of affected 
rows will be retrieved.
```
Integer result = database.update(newEmployee);

if (result != null) {
    System.out.println("Successfully updated " + result + " rows.");
}
else {
    System.out.println("Could not update row to table.");
}
```
To achieve asynchronous operation and react upon completion, use callbacks:
```
database.update(updatedEmployee, OnComplete<Integer>() {
    @Override
    public void onSuccess(Integer feedback) {
        
    }

    @Override
    public void onFailure() {

    }
});
```
The Integer parameter 'feedback' sent by the onSuccess callback denotes the amount of rows affected by the command.

### Delete (Delete)
Delete rows from table by passing a condition via SQL WHERE clause (possible queries: "name = 'Tears in Heaven'", 
"id = 1"):
```
database.delete("name = 'Rosie Miller'");
```
If you wish to test whether the command has been successfully executed, assign method return to an 'Integer' variable. 
A null result means the operation has NOT been successfully completed. If operation succeeds, the number of affected 
rows will be retrieved.
```
Integer result = database.delete("name = 'Rosie Miller'");

if (result != null) {
    System.out.println("Successfully deleted " + result + " rows.");
}
else {
    System.out.println("Could not delete row on table.");
}
```
To achieve asynchronous operation and react upon completion, use callbacks:
```
database.delete("name = 'Rosie Miller'", OnComplete<Integer>() {
    @Override
    public void onSuccess(Integer feedback) {

    }

    @Override
    public void onFailure() {

    }
});
```
The Integer parameter 'feedback' sent by the onSuccess callback denotes the amount of rows affected by the command.

## Extra Operations
This section will explain additional ways of extracting data from your database.

### GetFill (Left inner join, many-to-one)
The 'getFill' method allows the developer to perform a left inner join on the target table with minimum syntactic effort.
Let's suppose an employees table column 'dept' (for 'department') doesn't contain the department name directly, such as 
'IT' or  'Sales', but instead contains the foreign keys for the departments that are, in fact, contained in another table, say, 
the departments table. This is highly recommended for organizational purposes, since if one of the departments suddenly 
has it's name changed, you would simply need to change the corresponding row on the departments table, instead of 
changing each one of the rows in the employees table individually. However those keys have no informational value to the
user that will be checking the table, since it's not in his interest to see the employee's department key, but the actual
department name itself. Below is a demonstration of how our example tables could look like:
```
       EMPLOYEES TABLE ('employees_tbl')                DEPARTMENTS TABLE ('departments_tbl')
+--------------------------------------------+    +----------------------------------------------+
|  id   |        name        |     deptId    |    |   id   |  departmentName   |  isOutsourced   |
+--------------------------------------------+    +----------------------------------------------+
|   1       Marcus Garvey             1      |    |    1         Sales               false       |
|   2       Celina Gomes              2      |    |    2         IT                  false       |
|   3       Rosetta Stone             3      |    |    3         Deliveries          true        |
|   4       Robson Charles            2      |    +----------------------------------------------+
+--------------------------------------------+  
```
First step to accomplish this would be to create a model containing fields with the same name of the columns you wish 
to extract, even though the columns are distributed amongst different tables:
```
public class EmployeeNameAndDept {

    // Field names must match database table column names.
    private int id;
    private String name;
    private String departmentName;

    // For use with MySQLAccess, model has to implement parameterless constructor
    public EmployeeNameAndDept () {}
    
    @Override
    public String toString() {
        return "Employee { " +
                "id=" + id +
                ", name='" + name + '\'' +
                ", departmentName='" + departmentName + '\'' +
                '}';
    }
}
```
It's important to note that, except for the primary key column, all other columns must have unique names even if they 
are on different tables. Next step would be simply to execute the 'getFill' command as shown below:
```
List<EmployeeNameAndDept> employeesAndDepartmentNames = 
        database.getFill(EmployeeNameAndDept.class, "deptId", "departments_tbl");
```
The first parameter will inform the model to the algorithm as done on the other get methods. The second parameter
points the column in the target table that will be used as a reference to join the data which will be brought 
from the secondary table. The third parameter points to the table to be joined. The algorithm will automatically 
consider that the values of the reference columns on the target table point to the secondary table's primary key 
column. This command will produce, retrieve and map the following table:
```              
+-----------------------------------------------+
|  id   |        name        |  departmentName  | 
+-----------------------------------------------+ 
|   1       Marcus Garvey         Sales         | 
|   2       Celina Gomes          IT            | 
|   3       Rosetta Stone         Deliveries    | 
|   4       Robson Charles        IT            | 
+-----------------------------------------------+  
```
If you have multiple columns on your target table referencing other tables and you wish to perform multiple joins on 
one move, you can do this by passing an array of references in form of strings:
```
List<EmployeeNameDeptSalaryRangeAgeRange> employeeNameDeptSalaryRangeAgeRange = 
        database.getFill(EmployeeNameDeptSalaryRangeAgeRange.class,
                new String[]{"deptId", "salaryRangeId", "ageRangeId"},
                new String[]{"departments_tbl", "salary_range_tbl", "age_range_tbl"});
```

### GetMetrics
The 'getMetrics' commands will retrieve data about numeric columns and row counts on the target table:
```
Number count = database.getCount();
Number sumrev = database.getSum("salary");
Number avgrev = database.getAvg("salary");
Number maxdown = database.getMax("salary");
Number mindown = database.getMin("salary");
```
First overload will send and SQL WHERE clause to filter the results:
```
Number count = database.getCount("dept = 'IT'"); // Counts no. of employees on IT department
Number sumrev = database.getSum("salary", "salary>2000"); // Gets sum of salaries over 2000
Number avgrev = database.getAvg("salary", "dept = 'Sales'"); // Gets average salary on Sales department
...
```
For asynchronous execution add callback to the final parameter:
```
database.getCount(OnComplete<Number>() {
    @Override
    public void onSuccess(Number feedback) {
        
    }

    @Override
    public void onFailure() {

    }
});

database.getMin("salary", "dept = 'Sales'", OnComplete<Number>() {
    @Override
    public void onSuccess(Number feedback) {
        
    }

    @Override
    public void onFailure() {

    }
});
```

## Logging
MySQLAccess comes bundled with a native logging system, designed to show to the developer the inner workings of the 
module. Logging is activated system-wide and should be done before or right after module instantiation:
```
MySQLAccess.logDetails();
MySQLAccess.logInfo();
MySQLAccess.logFetch();
```
These loggers show the following information:
- logDetails: shows details about the data incoming and outgoing from and to the database.
- logInfo: shows the actions that the algorithm is taking to communicate with the database.
- logFetch: shows the information the module is gathering about the database and table properties.

Usage of the loggers is optional and recommended only during the development or debugging stages. You can activate one
or more loggers depending on what you need to monitor.