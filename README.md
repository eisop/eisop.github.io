# EISOP Checker Framework website

## Build and deployment instructions

- Check out `master` branch: `git checkout master`

- Build project: `mvn package`

- Check out `gh-pages` branch: `git checkout gh-pages`

- (Re-)build the website: `java -cp ./target/eisop.github.io-1.0-SNAPSHOT-jar-with-dependencies.jar io/github/eisop/website/EisopSiteGenerator`

- Review changes: `git status` and `git diff`

- Add and commit them: `git add afu/ cf/` and `git commit`

- Push website changes to `gh-pages` branch: `git push origin gh-pages`


## Development notes

- Format Java code: `mvn spotless:apply`

- Sort `pom.xml`: `mvn com.github.ekryd.sortpom:sortpom-maven-plugin:sort`


## Contact

Please address your questions and comments to
[Werner Dietl](https://ece.uwaterloo.ca/~wdietl/contact.html).
