---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: default
---

Annotation File Utilities

Annotation File Utilities
=========================

Contents:

*   [Motivation](#motivation)
    *   [External storage of annotations](#jaif-file)
    *   [Annotation File Utilities](#annotation-file-utilities-description)
*   [Installation](#installation)
    *   [Building from source](#viewing-source)
*   [Using the Annotation File Utilities](#using)
    *   [Insert-annotations](#insert-annotations)
    *   [Extract-annotations](#extract-annotations)
    *   [Insert-annotations-to-source](#insert-annotations-to-source)
        *   [Classpath](#insert-annotations-to-source-classpath)
        *   [Locations in source code must exist](#insert-annotations-to-source-locations)
        *   [Command-line options](#insert-annotations-to-source-command-line-options)
*   [Design and Implementation Details](#implementation)
    *   [Scene-lib](#scene-lib)
        *   [Bytecode Insertion](#bytecode-insertion)
        *   [Bytecode Extraction](#bytecode-extraction)
    *   [Annotation-file-utilities](#source-insertion)
*   [Feedback and bug reports](#feedback)
    *   [Changelog](#changelog)

* * *

Motivation
----------

Java annotations are meta-data about Java program elements, as in “`@Deprecated class Date { ... }`” or “`List<@NonNull String>`”. Ordinarily, Java annotations are written in the source code of a `.java` Java source file. When `javac` compiles the source code, it inserts the annotations in the resulting `.class` file (as “attributes”).

### External storage of annotations

Sometimes, it is convenient to specify the annotations outside the source code or the `.class` file. The document “Annotation File Format Specification” ([PDF](annotation-file-format.pdf), [HTML](annotation-file-format.html)) defines a textual format for annotations, and it also motivates reasons why such a file format is necessary in addition to the `.java` and `.class` formats. The file format supports both the declaration annotations and type annotations.

An annotation file conventionally has the extension `.jaif` (for Java Annotation Index File). The [`scene-lib`](#scene-lib) sub-project provides API methods for building and manipulating annotation files.

### Annotation File Utilities

Programmers need to be able to transfer annotations between the three possible locations for annotations — source code, class files, and annotation files. Programmers will want to extract annotations from source and class files to an annotation file in order to easily read annotations, while various tools will only read annotations from source and class files. The Annotation File Utilities provide three tools to read and write annotation files.

*   [`insert-annotations`](#insert-annotations) reads annotations from an annotation file and inserts them into an existing class file
*   [`extract-annotations`](#extract-annotations) reads annotations from a class file and writes them out to a new annotation file
*   [`insert-annotations-to-source`](#insert-annotations-to-source) reads annotations from an annotation file and inserts them into an existing Java source file

The diagram below shows how each tool moves annotations from one file format to another.

![Relationships between AFU tools](figures/tool-relations.svg)

There is no `extract-annotations-from-source` tool: one can compile the source code and then use `extract-annotations` to read the annotations from the class file.

* * *

Installation
------------

The following instructions assume either a Linux or Windows system using a command-line environment.

The current release is Annotation File Utilities version 3.42.0, December 20, 2024.

1.  Download [annotation-tools-3.42.0-eisop5.zip](annotation-tools-3.42.0-eisop5.zip).
2.  Create a directory named `annotation-tools` by unpacking the distribution zipfile. (You will typically make `annotation-tools/` a sibling of `checker-framework/`.)
    
        unzip annotation-tools-3.42.0.zip
    
3.  Add the `annotation-file-utilities` directory to your path.
    *   For **Unix** (including Linux and MacOS), add the directory to your PATH environment variable. If your shell is sh or bash, add to your `~/.bashrc` or `~/.bash_profile` file:
        
            export PATH=${PATH}:/path/to/annotation-tools/annotation-file-utilities/scripts
        
    *   For **Windows**, add the directory to your `PATH` system variable by going to
        
             Control Panel -> System -> Advanced -> Environment Variables 
        
        From there, find the `PATH` variable under “System variables” and append to it the directory `_path\to_\annotatation-tools\annotation-file-utilities\scripts`.

### Building from source

The annotation file utilities are pre-compiled (a jar file is included in the distribution), so most users do not need to compile it themselves.

There are two ways to obtain the source code. Source code is provided in the [distribution](https://github.com/typetools/annotation-tools/releases). Alternately, see the source code repository at [https://github.com/typetools/annotation-tools](https://github.com/typetools/annotation-tools).

To compile and run tests, do `./gradlew build` from the `annotation-file-utilities` subdirectory.

* * *

Using the Annotation File Utilities
-----------------------------------

To use the tools, simply run them from the command-line with the appropriate arguments. The following instructions are for running the tools on a Linux/Unix/MacOS machine. The tools work identically on Windows, except the extension `.bat` needs to be appended to the tool name (for example, Windows users would execute `insert-annotations.bat` instead of `insert-annotations`).

For all the tools, arguments starting with a single ‘`@`’ are recognized as argument files (`argfiles`), the contents of which get expanded into the command line. (Initial `@@` represents a literal `@` in the argument text.) For additional details of argfile processing, refer to Oracle's [`javac`](https://docs.oracle.com/javase/7/docs/technotes/tools/windows/javac.html) documentation.

### Insert-annotations

To insert annotations specified by an annotation file into a class file, use the insert-annotations tool. Running:

    insert-annotations mypackage.MyClass indexFile.jaif

will read in all the annotations from the annotation file `indexFile.jaif` and insert those annotations pertaining to `mypackage.myClass` into the class file for `mypackage.MyClass`, outputting the final class file to `mypackage.MyClass.class` in the present working directory. Note that the class file for `mypackage.MyClass` must be located on your classpath.

Multiple pairs of class and index files (in that order) can be specified on a single command line; if the program exits normally, the results are the same as if the program were run once for each pair of arguments in sequence. Run:

    insert-annotations --help

for usage information. In addition to the command-line arguments mentioned there, you can also set the classpath via the `-cp` or `--classpath` command-line option.

### Extract-annotations

To extract annotations from a class file and write them to an annotation file, use the extract-annotations tool. Running:

    extract-annotations mypackage.MyClass

will locate the class file for `mypackage.MyClass`, read all annotations from it, and write the results in annotation file format to `mypackage.MyClass.jaif` in the present working directory. Note that `mypackage.MyClass` must be located on your classpath. Alternately, you can specify a classfile directly:

    extract-annotations /path/to/MyClass.class

Multiple classes or classfiles can be specified on a single command line; if the program exits normally, the results are the same as if the program was run once for each class in sequence.

Run:

    extract-annotations --help

for usage information. In addition to the command-line arguments mentioned there, the `-cp` and `-classpath` command-line options set the classpath to use to look up annotations.

### Insert-annotations-to-source

To insert annotations specified by an annotation file into a Java source file, use the insert-annotations-to-source tool. Running:

    insert-annotations-to-source index1.jaif index2.jaif mypackage/MyClass.java yourpackage/YourClass.java

will read all the annotations from `index1.jaif` and `index2.jaif`, insert them (when applicable) into their appropriate locations in `mypackage/MyClass.java` and `yourpackage/YourClass.java`, and write the results to `annotated/mypackage/MyClass.java` and `annotated/mypackage/MyClass.java`, respectively.

Index and source files can be specified in any order, mixing the two file types freely; if the source files have no overlapping definitions and the program exits normally, the results are the same as if the program were run once for each source file, with _all_ JAIFs given for each run.

The command-line arguments appear [below](#insert-annotations-to-source-command-line-options).

#### Classpath

Your classpath must include classes that are arguments to annotations. For example, to insert `@A(element = B.class)`, your classpath must contain `B.class`.

If you wish to insert annotations into method bodies, you must have the associated class `mypackage.MyClass.class` on your classpath. You can insert annotations on class/field/method declarations and signatures without the class on your classpath.

#### Locations in source code must exist

If the `.jaif` file contains annotations for a type parameter, but the source code uses a raw type, then you will get an error such as

Found class Edge, but unable to insert @checkers.nullness.quals.Nullable:
  @checkers.nullness.quals.Nullable (nl=true) @ \[GenericArrayLocationCriterion at ( \[TYPE\_ARGUMENT(0)\] ), ...

In this case, you should add type arguments, such as changing

  public void pushNonezeroRing(Stack stack, Hashtable seen) {

to

  public void pushNonezeroRing(Stack<Edge> stack, Hashtable<Edge, ?> seen) {

In the following cases, insert-annotations-to-source will generate code to provide a location for an annotation:

*   method and constructor [receivers;](https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.4.1-220)
*   type parameter bounds (`extends Object`);
*   type casts for expressions
*   nullary constructor definitions; and
*   explicit constructor invocations for literal arrays (e.g., `{"a", "b"}` becomes `new String[] {"a", "b"}`).

#### Command-line options

The `-cp` and `-classpath` command-line options set the classpath to use to look up classes and annotations. The other command-line options appear below and are also available by running `insert-annotations-to-source --help`.

*   General options
    *   **\-d** **\--outdir=**_directory_. Directory in which output files are written. \[default: annotated/\]
    *   **\-i** **\--in-place=**_boolean_. If true, overwrite original source files (making a backup first). Furthermore, if the backup files already exist, they are used instead of the .java files. This behavior permits a user to tweak the `.jaif` file and re-run the annotator.
        
        Note that if the user runs the annotator with --in-place, makes edits, and then re-runs the annotator with this --in-place option, those edits are lost. Similarly, if the user runs the annotator twice in a row with --in-place, only the last set of annotations will appear in the codebase at the end.
        
        To preserve changes when using the --in-place option, first remove the backup files. Or, use the `-d .` option, which makes (and reads) no backup, instead of --in-place. \[default: false\]
        
    *   **\-a** **\--abbreviate=**_boolean_. If true, insert `import` statements as necessary. \[default: true\]
    *   **\-c** **\--comments=**_boolean_. Insert annotations in comments \[default: false\]
    *   **\-o** **\--omit-annotation=**_string_. Omit given annotation
    *   **\--nowarn=**_boolean_. Suppress warnings about disallowed insertions \[default: false\]
    *   **\--convert-jaifs=**_boolean_. Convert JAIFs to AST Path format, but do no insertion into source \[default: false\]
    *   **\-h** **\--help=**_boolean_. Print usage information and exit \[default: false\]
*   Debugging options
    *   **\-v** **\--verbose=**_boolean_. Verbose (print progress information) \[default: false\]
    *   **\--debug=**_boolean_. Debug (print debug information) \[default: false\]
    *   **\--print-error-stack=**_boolean_. Print error stack \[default: false\]

* * *

Design and Implementation Details
---------------------------------

This section describes some high level-design and implementation details of the Annotation File Utilities, including the different components of the Annotation File Utilities and how they fit together. It is intended for someone who is beginning work on the Annotation File Utilities or is curious about how the Annotation File Utilities work.

The Annotation File Utilities is composed of two sub-projects: `scene-lib` and `annotation-file-utilities`. The `scene-lib` sub-project represents a `.jaif` file and inserts and extracts annotations to/from bytecode. The `annotation-file-utilities` sub-project inserts annotations into source code.

### Scene-lib

`scene-lib` is an interface to a `.jaif` file. It reads in and writes out `.jaif` files and provides an internal representation of a `.jaif` file to access and manipulate.

Internally, a `.jaif` file is represented by the `scene-lib/src/annotations/el/AScene.java` class. The `AScene` class (or “annotated scene”) roughly parallels the root of an abstract syntax tree. An `AScene` has a number of classes (`AClass`) as children. Each class has a number of methods (`AMethod`), fields (`AElement`), etc. as children. All of these classes are related in the type hierarchy shown below.

![scene-lib type hierarchy](figures/scene-lib-type-hierarchy.svg)

Each class in the type hierarchy has one or more fields to hold annotations for the different components of the class. For example, the `AMethod` class has the following fields: bounds, return type, receiver parameters, and throws clause. Each of these fields holds the annotations stored on that part of the method. For details on the remainder of the classes in the type hierarchy, and their respective fields, see the documentation for each file in `scene-lib/src/annotations/el/`.

An `AScene` instance can be created in two ways. An empty `AScene` can be created by calling the `AScene` constructor, or an `AScene` can be created by parsing an existing `.jaif` file. Once an `AScene` is created, annotations can be added to it by adding them to the correct fields of the children. An `AScene` can also be output to create a new `.jaif` file.

#### Bytecode Insertion

Annotations can be inserted into bytecode by executing the `annotation-file-utilities/scripts/insert-annotations` script. This script takes one or more ⟨class name, `.jaif` file⟩ pairs as arguments. The annotations specified in the `.jaif` file are inserted into the classfile directly before the `.jaif` file in the argument list.

First, each `.jaif` file is parsed into an `AScene` (as described in [Scene-lib](#scene-lib)). Then, ASM's `ClassReader.java` parses the classfile. As it is parsing the classfile, it passes the parsed bytecode off to the `scene-lib/src/annotations/io/classfile/ClassAnnotationSceneWriter.java` class. This class has a reference to the `AScene` parsed from the `.jaif` file. As this class receives the parsed bytecode it inserts the relevant annotations from the `AScene` in the bytecode and then writes the bytecode back out.

#### Bytecode Extraction

Annotations can be extracted from bytecode by executing the `annotation-file-utilities/scripts/extract-annotations` script. This script takes one or more class names as arguments and outputs the annotations found in those classes to `.jaif` files.

First, an empty `AScene` is constructed to store the annotations. ASM's `ClassReader.java` parses the classfile and passes the parsed bytecode off to the `scene-lib/src/annotations/io/classfile/ClassAnnotationSceneReader.java` class. This class filters out the annotations in the bytecode and adds them to the correct part of the `AScene`. After this, the `AScene` is output to a `.jaif` file.

### Annotation-file-utilities

The `annotation-file-utilities` sub-project inserts annotations into source code. It can be run by executing the `annotation-file-utilities/scripts/insert-annotations-to-source` script. The script takes one or more `.jaif` files, followed by one or more `.java` source files as arguments. The annotations in the `.jaif` files are inserted into the `.java` source files.

First, an instance of `annotation-file-utilities/src/org/checkerframework/afu/annotator/specification/IndexFileSpecification.java` is created. Its `parse` method parses the `.jaif` file into an `AScene` (as described in [Scene-lib](#scene-lib)). The `parse` method calls the `parseScene` method, which traverses through the `AScene` and creates an `annotation-file-utilities/src/org/checkerframework/afu/annotator/specification/CriterionList.java.` A `CriterionList` identifies a unique AST node that is the location of an insertion. It contains objects that implement the `annotation-file-utilities/src/org/checkerframework/afu/annotator/find/Criterion.java` interface. Each `Criterion` has an `isSatisifiedBy` method — a predicate that takes an AST node and returns `true` if the AST node satisfies the `Criterion` and `false` otherwise. To determine if a given node matches a `CriterionList`, the node is passed to all of the `Criterion`s in the `CriteriaList`. If every `Criterion` returns `true` then it is match. If one or more `Criterion`s return `false` then it is not a match. The various `Criterion` classes are in the `annotation-file-utilities/src/org/checkerframework/afu/annotator/find/` directory. For example, take the following source code:

package afu.example;

public class Test {
    public void m(boolean b, int i) {
      // ...
    }
}

The `CriterionList` to specify the location of the `i` parameter contains the following `Criterion`s:

*   `InPackageCriterion("afu.example")`
*   `InClassCriterion("Test")`
*   `InMethodCriterion("m(ZI)V")`
*   `ParamCriterion(1)`

After this `CriterionList` is built up an `annotation-file-utilities/org/checkerframework/afu/src/annotator/find/Insertion.java` is created. An `Insertion` stores an `annotation-file-utilities/org/checkerframework/afu/src/annotator/find/Criteria.java` (which is created from a `CriterionList`) and the text to be inserted. All of these `Insertion`s are then added to a list. The Java compiler then is called to parse the Java source into an abstract syntax tree. This is followed by a call to the `getPositions` method of `annotation-file-utilities/src/org/checkerframework/afu/annotator/find/TreeFinder.java`, which scans through each node of the abstract syntax trees. For each node, it runs through the `Criteria` for each un-matched `Insertion`. If at least one of the `Criteria` does not match, then this is not the correct place for the `Insertion` and the `Insertion` will be checked at the remaining nodes of the tree. If all of the `Criteria` match, then this node is the correct place for the `Insertion`. It is removed from the list of un-matched `Insertion`s and the position where to insert the `Insertion` is determined. This position is the integer index in the file where the `Insertion` should be inserted. After the positions are found for all of the `Insertion`s, the `Insertion` text is inserted into the file. This happens backwards, with `Insertion`s at the end of the file (i.e. with higher positions) being inserted first. If `Insertion`s were instead inserted from the beginning of the file then a single `Insertion` would invalidate all of the positions for the following `Insertion`s.

If there are remaining `Insertion`s that were not matched to a node in the abstract syntax tree then an error message is displayed.

* * *

Feedback and bug reports
------------------------

To submit a bug report or request a new feature, use the [issue tracker](https://github.com/typetools/annotation-tools/issues). When reporting a bug, please include exact instructions in how to reproduce it, and please also attach relevant input files. This will let us resolve the issue quickly.

You can also reach the developers at [annotation-tools-dev@googlegroups.com](mailto:annotation-tools-dev@googlegroups.com). But please use the [issue tracker](https://github.com/typetools/annotation-tools/issues) for bug reports and feature requests.

### Changelog

The [changelog](changelog.html) describes what is new in each release.

* * *