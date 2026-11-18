# EISOP Checker Framework website

## Publishing the website

The website is updated automatically via a daily GitHub Actions schedule, at
20:00 UTC, that checks if new Checker Framework or Annotation File Utilities
releases or a new nightly build have been published. If so, it generates and
publishes the pages.

A release of the Checker Framework has two zip assets,
`checker-framework-VERSION.zip` and `annotation-file-utilities-VERSION.zip`.
The former repository `eisop/annotation-tools` is archived; its releases,
with zips named `annotation-tools-VERSION.zip`, are used for the Annotation
File Utilities versions before the first release of the Checker Framework that
has the second asset.

The development version of the site, https://eisop.github.io/cf/dev/ and
https://eisop.github.io/afu/dev/, is the nightly build of master: the
`nightly` prerelease of `eisop/checker-framework`, which the `nightly-zips` job
of its CI workflow updates after the daily scheduled run (07:00 UTC, started
hours late by GitHub) passes on a master that has changed.  Prereleases are
otherwise ignored.  `cf/dev/build.txt` holds the time of the published nightly
build, so the daily run publishes a new one, and only a new one.  The front
pages link to it, and `robots.txt` keeps search engines out of it.

You can also run the **Publish website** workflow manually from the Actions tab.
It checks out `master` for the generator, builds it, checks out `gh-pages`,
downloads any release that is not on the site yet, regenerates the pages, and pushes.

Manual runs default to a dry run: everything except the push, with the resulting
`git status` in the run summary. Read that, then run it again with the dry-run
box unchecked to publish.

### Doing it by hand

```
git checkout master
mvn package
git checkout gh-pages
git checkout master -- index.md 404.html robots.txt _config.yml
java -cp ./target/eisop.github.io-1.0-SNAPSHOT-jar-with-dependencies.jar \
  io.github.eisop.website.EisopSiteGenerator
git status && git diff                 # review
git add afu/ cf/ index.md 404.html robots.txt _config.yml && git commit
git push origin gh-pages
```

The page templates live in `src/main/resources/` and are packaged into the jar,
so the generator always uses the templates belonging to the code you built.
The hand-written pages `index.md` and `404.html`, `robots.txt`, and the Jekyll
configuration `_config.yml` are edited on `master` and copied to `gh-pages` when publishing;
a push to `master` that changes one of them publishes the site.

### Pre-release validation

You can test a locally-built release zip before publishing or creating a GitHub release:

```
java -cp ./target/eisop.github.io-1.0-SNAPSHOT-jar-with-dependencies.jar \
  io.github.eisop.website.EisopSiteGenerator \
  --local-release /path/to/checker-framework-<version>.zip \
  --only-latest
```

- `--local-release <zip>`: Substitutes the local zip file as the newest release entry, extracting its version from the file name.
- `--only-latest`: Builds only the newest release's folder and the `cf/` top level, skipping downloads of past release archives.

To inspect and link-check the output, serve the site root with a local HTTP server (site-absolute links like `/cf/...` require a server root and cannot be checked via `file://`):

```bash
# Serve the working directory (site root)
python3 -m http.server 8000

# Validate both depths with a link checker:
lychee http://localhost:8000/cf/index.html
lychee http://localhost:8000/cf/<release>/index.html
```


## Development notes

- Format Java code: `mvn spotless:apply`

- Sort `pom.xml`: `mvn com.github.ekryd.sortpom:sortpom-maven-plugin:sort`


## Contact

Please address your questions and comments to
[Werner Dietl](https://ece.uwaterloo.ca/~wdietl/contact.html).
