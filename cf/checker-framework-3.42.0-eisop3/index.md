---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: default
---

 The Checker Framework  ![Checker Framework logo](CFLogo.png)

The Checker Framework
=====================

Are you tired of null pointer exceptions, unintended side effects, SQL injections, concurrency errors, mistaken equality tests, and other run-time errors that appear during testing or in the field?

The Checker Framework enhances Java's type system to make it more powerful and useful. This lets software developers detect and prevent errors in their Java programs. The Checker Framework includes compiler plug-ins ("checkers") that find bugs or verify their absence. It also permits you to write your own compiler plug-ins.

*   Quick start: see the [**Installation instructions and tutorial**](manual/manual.html#installation).
*   Download: [checker-framework-3.42.0-eisop3.zip](/cf/checker-framework-3.42.0-eisop3.zip) (March 02, 2024); includes source, platform-independent binary, tests, and documentation.  
    Then, see the [**installation instructions and tutorial**](manual/manual.html#installation).
*   Documentation:
    *   [Checker Framework Manual (HTML)](manual/manual.html)
    *   [Checker Framework Manual (PDF)](manual/manual.pdf)
    *   [Installation instructions](manual/manual.html#installation)  
        or, try it without installation at the [Checker Framework Live Demo](http://eisop.uwaterloo.ca/live/) webpage
    *   [Tutorial](tutorial/) with Nullness Checker, Regex Checker, and Tainting checker  
        (There is also an older external [Nullness Checker tutorial](https://github.com/glts/safer-spring-petclinic/wiki) whose setup information is out of date.)
    *   [FAQ (Frequently Asked Questions with answers)](manual/manual.html#faq)
    *   [Javadoc](api/checker-javadoc/) API documentation
    *   [Changelog](CHANGELOG.md)
*   Source code repository (at GitHub): [https://github.com/eisop/checker-framework/](https://github.com/eisop/checker-framework/)  
    The Checker Framework Manual contains [instructions on building from source](manual/manual.html#build-source).  
    Also see the [Developer manual](https://htmlpreview.github.io/?https://github.com/eisop/checker-framework/master/docs/developer/developer-manual.html).
*   Inference tools automatically add annotations to your code, making it even easier to start using the checkers. The Checker Framework manual contains [a list of inference tools](manual/manual.html#type-inference-tools).
*   Optional related tools:
    *   The [**Annotation File Utilities**](../afu/annotation-file-utilities.md) extract annotations from, and write annotations to, `.java` and `.class` files. It also provides a representation (called an “annotation file”) for annotations that is outside the source code or the `.class` file. The tools support both Java 5 declaration annotations and Java 8 type annotations.
        *   [$LatestAnnotationFileUtilitiesReleaseZip]($LatestAnnotationFileUtilitiesReleaseDownloadLink) ($LatestAnnotationFileUtilitiesReleaseDate)
        *   [source code repository](https://github.com/eisop/annotation-tools/)
        *   [Documentation](../afu/annotation-file-utilities.html) is included in the zip archive and in the repository.
*   [Archive of previous releases](releases/releases.html) of the Checker Framework
*   Research papers: See the [Checker Framework manual](manual/manual.html#publications)

* * *

Support and community
---------------------

If you **have a question**, then first see whether your question is answered in one of the manuals listed under [Documentation](#documentation) below. If none of those documents answers your question, then use one of the [mailing lists](#mailing-lists).

### Documentation

*   Checker Framework Manual ([PDF](manual/manual.pdf), [HTML](manual/manual.html))
*   [Installation instructions](manual/manual.html#installation)  
    or, try it without installation at the [Checker Framework Live Demo](http://eisop.uwaterloo.ca/live/) webpage
*   [Tutorial](tutorial/) Other tutorials:
    *   [Nullness Checker tutorial](https://github.com/glts/safer-spring-petclinic/wiki) (external site, setup information is out of date)
*   [FAQ (Frequently Asked Questions with answers)](manual/manual.html#faq)
*   [Javadoc](api/checker-javadoc/) API documentation
*   [Changelog](CHANGELOG.md)

### Bug reports

If you encounter a problem, please submit a bug report so that we can fix it. To submit a bug report, read these [instructions](manual/manual.html#reporting-bugs), and then use the [Checker Framework issue tracker](https://github.com/eisop/checker-framework/issues).

### Mailing lists

We welcome questions, suggestions, patches, reports about case studies, and other contributions. Please let us know how we can improve the Checker Framework!

*   [checker-framework-discuss](https://groups.google.com/forum/#!forum/checker-framework-discuss): for general discussion about the Checker Framework for building pluggable type systems ([view archives](https://groups.google.com/forum/#!forum/checker-framework-discuss/topics), [view old archives](https://types.cs.washington.edu/list-archives/jsr308/))
*   [checker-framework-dev](https://groups.google.com/forum/#!forum/checker-framework-dev): to reach the developers who maintain and extend the Checker Framework ([view archives](https://groups.google.com/forum/#!forum/checker-framework-dev/topics), [view old archives](https://types.cs.washington.edu/list-archives/checkers/))

You can also use the mailing lists to **give help**. Here are just a few examples:

*   Respond to questions.
*   Report problems (in the implementation or the documentation) or request features.
*   Write code, then share your bug fixes, new features, compiler plug-ins, or other improvements.
*   Make suggestions regarding the specification.

Another way to help is to tell your friends and colleagues about the usefulness and practicality of type annotations, or to report your successes to the mailing lists.

* * *

Last updated: March 02, 2024