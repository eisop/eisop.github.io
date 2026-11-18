/* (C)2026 */
package io.github.eisop.website;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Tests for the parts of {@link EisopSiteGenerator} that do not need the network. */
class EisopSiteGeneratorTest {

    @Test
    void readableDateFromGitHubTimestamp() {
        assertEquals(
                "April 26, 2026", EisopSiteGenerator.getReadableDate("2026-04-26T23:05:21Z"));
        assertEquals(
                "December 20, 2024", EisopSiteGenerator.getReadableDate("2024-12-20T23:39:45Z"));
        assertEquals(
                "January 31, 2025", EisopSiteGenerator.getReadableDate("2025-01-31T00:00:00Z"));
    }

    @Test
    void themeShippedPageKeepsOnlyTheBody() {
        String html =
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head><title>T</title></head>\n"
                        + "<BODY class=\"x\">\n<p>Download: <a href=\"/cf/a.zip\">a.zip</a></p>\n"
                        + "</Body>\n</html>\n";
        assertEquals(
                "---\nlayout: default\n---\n<p>Download: <a href=\"/cf/a.zip\">a.zip</a></p>\n",
                EisopSiteGenerator.themeShippedPage(html));
    }

    @Test
    void themeShippedPageWithoutBodyTagsKeepsEverything() {
        assertEquals(
                "---\nlayout: default\n---\n<p>text</p>\n",
                EisopSiteGenerator.themeShippedPage("  <p>text</p>  "));
    }

    @Test
    void templatesArePackaged() throws IOException {
        String cf = EisopSiteGenerator.readTemplate("cf-template.md");
        assertTrue(cf.contains("$LatestCheckerFrameworkReleaseZip"));
        assertTrue(cf.contains("$LatestAnnotationFileUtilitiesReleaseDownloadLink"));
        String afu = EisopSiteGenerator.readTemplate("/afu-template.md");
        assertTrue(afu.contains("$LatestAnnotationFileUtilitiesReleaseZip"));
    }

    @Test
    void missingTemplateIsAnError() {
        assertThrows(IOException.class, () -> EisopSiteGenerator.readTemplate("no-such.md"));
    }

    @Test
    void afuPlaceholdersAreFilledOnEveryReleasePage(@TempDir Path cfDir) throws IOException {
        String page =
                "[$LatestAnnotationFileUtilitiesReleaseZip]"
                        + "($LatestAnnotationFileUtilitiesReleaseDownloadLink)"
                        + " ($LatestAnnotationFileUtilitiesReleaseDate)\n";
        String filled = "[afu-1.zip](/afu/afu-1.zip) (May 1, 2026)\n";
        Path top = write(cfDir.resolve("index.md"), page);
        Path release = write(cfDir.resolve("checker-framework-1/index.md"), page);
        Path other = write(cfDir.resolve("checker-framework-1/other.md"), page);

        for (int run = 0; run < 2; run++) {
            // The second run checks that the substitution is idempotent.
            EisopSiteGenerator.substituteAfuPlaceholders(
                    cfDir.toFile(), "afu-1.zip", "/afu/afu-1.zip", "May 1, 2026");
            assertEquals(filled, read(top));
            assertEquals(filled, read(release));
            assertEquals(page, read(other));
        }
    }

    @Test
    void afuPlaceholdersToleratesMissingPages(@TempDir Path cfDir) throws IOException {
        Files.createDirectories(cfDir.resolve("checker-framework-1"));
        EisopSiteGenerator.substituteAfuPlaceholders(
                cfDir.toFile(), "afu-1.zip", "/afu/afu-1.zip", "May 1, 2026");
        EisopSiteGenerator.substituteAfuPlaceholders(
                new File(cfDir.toFile(), "missing"), "afu-1.zip", "/afu/afu-1.zip", "May 1, 2026");
    }

    @Test
    void findZipAssetMatchesPrefixAndExtension() throws ParseException {
        JSONObject release =
                parse(
                        "{\"assets\": [{\"name\": \"checker-framework-1.zip.asc\"},"
                                + " {\"name\": \"checker-1.jar\"},"
                                + " {\"name\": \"annotation-file-utilities-1.zip\"},"
                                + " {\"name\": \"checker-framework-1.zip\"}]}");
        assertEquals(
                "checker-framework-1.zip",
                EisopSiteGenerator.findZipAsset(release, "checker-framework-").get("name"));
        assertEquals(
                "annotation-file-utilities-1.zip",
                EisopSiteGenerator.findZipAsset(release, "annotation-file-utilities-")
                        .get("name"));
        assertNull(EisopSiteGenerator.findZipAsset(release, "annotation-tools-"));
    }

    @Test
    void findZipAssetWithoutAssets() throws ParseException {
        assertNull(EisopSiteGenerator.findZipAsset(parse("{}"), "checker-framework-"));
        assertNull(
                EisopSiteGenerator.findZipAsset(parse("{\"assets\": []}"), "checker-framework-"));
    }

    @Test
    void afuDocPathAcceptsBothZipLayouts() {
        assertEquals(
                "changelog.html",
                EisopSiteGenerator.afuDocPath("annotation-file-utilities/changelog.html"));
        assertEquals(
                "annotation-file-format.pdf",
                EisopSiteGenerator.afuDocPath(
                        "annotation-file-utilities-1/annotation-file-utilities/"
                                + "annotation-file-format.pdf"));
        assertEquals(
                "figures/a.png",
                EisopSiteGenerator.afuDocPath("annotation-file-utilities/figures/a.png"));
    }

    @Test
    void afuDocPathRejectsOtherEntries() {
        assertNull(EisopSiteGenerator.afuDocPath("annotation-file-utilities/README"));
        assertNull(EisopSiteGenerator.afuDocPath("annotation-file-utilities/figures/sub/a.svg"));
        assertNull(EisopSiteGenerator.afuDocPath("annotation-file-utilities/tests/changelog.html"));
        assertNull(EisopSiteGenerator.afuDocPath("a/b/annotation-file-utilities/changelog.html"));
        assertNull(EisopSiteGenerator.afuDocPath("annotation-file-format.html"));
    }

    @Test
    void extractAfuDocsReplacesDocumentsInTheZip(@TempDir Path tmp) throws IOException {
        String dir = "annotation-file-utilities-2/annotation-file-utilities/";
        Path zip = tmp.resolve("annotation-file-utilities-2.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            addEntry(out, dir + "annotation-file-format.html", "new spec");
            out.putNextEntry(new ZipEntry(dir + "figures/"));
            out.closeEntry();
            addEntry(out, dir + "figures/a.svg", "svg");
            addEntry(out, dir + "figures/sub/b.svg", "nested");
            addEntry(out, dir + "tests/changelog.html", "test input");
        }
        Path afuDir = write(tmp.resolve("afu/annotation-file-format.html"), "old spec").getParent();
        write(afuDir.resolve("changelog.html"), "old changelog");

        EisopSiteGenerator.extractAfuDocs(zip.toFile(), afuDir.toFile());

        assertEquals("new spec", read(afuDir.resolve("annotation-file-format.html")));
        assertEquals("svg", read(afuDir.resolve("figures/a.svg")));
        assertEquals("old changelog", read(afuDir.resolve("changelog.html")));
        assertFalse(Files.exists(afuDir.resolve("figures/sub")));
        assertFalse(Files.exists(afuDir.resolve("tests")));
    }

    @Test
    void extractZipExtractsEveryEntry(@TempDir Path tmp) throws IOException {
        Path zip = tmp.resolve("r.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            addEntry(out, "r/a.txt", "a");
            addEntry(out, "r/d/b.txt", "b");
        }
        Path destination = tmp.resolve("out");
        EisopSiteGenerator.extractZip(zip.toFile(), destination.toFile());
        assertEquals("a", read(destination.resolve("r/a.txt")));
        assertEquals("b", read(destination.resolve("r/d/b.txt")));
    }

    @Test
    void extractZipRejectsATruncatedZip(@TempDir Path tmp) throws IOException {
        Path zip = write(tmp.resolve("r.zip"), "not a zip");
        assertThrows(
                IOException.class,
                () -> EisopSiteGenerator.extractZip(zip.toFile(), tmp.resolve("out").toFile()));
    }

    @Test
    void extractZipRejectsAnEncryptedZip(@TempDir Path tmp) throws IOException {
        Path content = write(tmp.resolve("a.txt"), "a");
        Path zip = tmp.resolve("r.zip");
        ZipParameters parameters = new ZipParameters();
        parameters.setEncryptFiles(true);
        parameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
        try (ZipFile zipFile = new ZipFile(zip.toFile(), "secret".toCharArray())) {
            zipFile.addFile(content.toFile(), parameters);
        }
        Path destination = tmp.resolve("out");
        IOException e =
                assertThrows(
                        IOException.class,
                        () -> EisopSiteGenerator.extractZip(zip.toFile(), destination.toFile()));
        assertTrue(e.getMessage().contains("encrypted"));
        assertFalse(Files.exists(destination));
    }

    @Test
    void writeRedirectKeepsQueryAndFragment(@TempDir Path tmp) throws IOException {
        Path page = tmp.resolve("manual/index.html");
        EisopSiteGenerator.writeRedirect(page.toFile(), "manual.html");
        String html = read(page);
        assertTrue(html.contains("<meta http-equiv=\"refresh\" content=\"0; url=manual.html\">"));
        assertTrue(
                html.contains(
                        "location.replace('manual.html' + location.search + location.hash);"));
        assertTrue(html.contains("<a href=\"manual.html\">"));
    }

    @Test
    void publishedReleasesDropsDraftsAndPrereleases() throws ParseException {
        JSONArray releases =
                (JSONArray)
                        new JSONParser()
                                .parse(
                                        "[{\"tag_name\": \"nightly\", \"prerelease\": true},"
                                                + " {\"tag_name\": \"draft\", \"draft\": true},"
                                                + " {\"tag_name\": \"a\", \"prerelease\": false},"
                                                + " {\"tag_name\": \"b\"}]");
        JSONArray published = EisopSiteGenerator.publishedReleases(releases);
        assertEquals(2, published.size());
        assertEquals("a", ((JSONObject) published.get(0)).get("tag_name"));
        assertEquals("b", ((JSONObject) published.get(1)).get("tag_name"));
        assertEquals(
                "nightly",
                EisopSiteGenerator.findRelease(releases, "nightly").get("tag_name"));
        assertNull(EisopSiteGenerator.findRelease(published, "nightly"));
    }

    @Test
    void devFrontPageSaysWhatItIsAndLinksToTheNightlyBuild() {
        String page =
                EisopSiteGenerator.devFrontPage(
                        "---\nlayout: default\n---\n"
                                + "Download: <a href=\"/cf/checker-framework-1.0.zip\">"
                                + "checker-framework-1.0.zip</a>\n"
                                + "<a href=\"/afu/annotation-file-utilities.html\">AFU</a>\n",
                        "checker-framework-1.1-SNAPSHOT.zip",
                        "https://example.org/n.zip",
                        "https://example.org/nightly",
                        "November 17, 2026");
        assertTrue(page.startsWith("---\nlayout: default\n---\n<p><strong>This is the development"));
        assertTrue(page.contains("November 17, 2026"));
        assertTrue(page.contains("<a href=\"https://example.org/nightly\">nightly build</a>"));
        assertTrue(
                page.contains(
                        "<a href=\"https://example.org/n.zip\">checker-framework-1.1-SNAPSHOT.zip</a>"));
        assertFalse(page.contains("checker-framework-1.0.zip"));
        assertTrue(page.contains("<a href=\"/afu/dev/annotation-file-utilities.html\">"));
    }

    @Test
    void generateDevSitePublishesTheNightlyBuild(@TempDir Path tmp) throws Exception {
        String cfName = "checker-framework-1.1-SNAPSHOT";
        Path cfZip = tmp.resolve(cfName + ".zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(cfZip))) {
            addEntry(out, cfName + "/docs/manual/manual.html", "manual");
            addEntry(
                    out,
                    cfName + "/docs/checker-framework-webpage.html",
                    "<html><body>Download: <a href=\"/cf/checker-framework-1.0.zip\">"
                            + "checker-framework-1.0.zip</a></body></html>");
            addEntry(out, cfName + "/checker/dist/checker.jar", "jar");
        }
        String afuName = "annotation-file-utilities-1.1-SNAPSHOT";
        Path afuZip = tmp.resolve(afuName + ".zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(afuZip))) {
            String dir = afuName + "/annotation-file-utilities/";
            addEntry(out, dir + "annotation-file-utilities.html", "afu page");
            addEntry(out, dir + "annotation-file-format.html", "afu format");
        }
        JSONObject nightly =
                parse(
                        "{\"tag_name\": \"nightly\", \"prerelease\": true,"
                                + " \"html_url\": \"https://example.org/nightly\", \"assets\": ["
                                + asset(cfZip, "2026-11-17T04:30:00Z")
                                + ", "
                                + asset(afuZip, "2026-11-17T04:30:01Z")
                                + "]}");
        Path cfDir = tmp.resolve("site/cf");
        Path afuDir = tmp.resolve("site/afu");

        EisopSiteGenerator.generateDevSite(nightly, cfDir.toFile(), afuDir.toFile());

        Path dev = cfDir.resolve("dev");
        assertEquals("manual", read(dev.resolve("manual/manual.html")));
        assertTrue(Files.exists(dev.resolve("manual/index.html")));
        String page = read(dev.resolve("index.html"));
        assertTrue(page.contains("This is the development version"));
        assertTrue(page.contains(cfZip.toUri().toString()));
        assertFalse(Files.exists(dev.resolve("checker")));
        assertFalse(Files.exists(dev.resolve("docs")));
        assertEquals("2026-11-17T04:30:00Z\n", read(dev.resolve("build.txt")));
        assertEquals("afu page", read(afuDir.resolve("dev/annotation-file-utilities.html")));
        assertEquals("afu format", read(afuDir.resolve("dev/annotation-file-format.html")));
        assertTrue(Files.exists(afuDir.resolve("dev/index.html")));

        // The same build is not published again.
        Files.delete(dev.resolve("manual/manual.html"));
        EisopSiteGenerator.generateDevSite(nightly, cfDir.toFile(), afuDir.toFile());
        assertFalse(Files.exists(dev.resolve("manual/manual.html")));
    }

    @Test
    void generateDevSiteWithoutANightlyBuildChangesNothing(@TempDir Path tmp) throws IOException {
        Path page = write(tmp.resolve("cf/dev/index.html"), "old");
        EisopSiteGenerator.generateDevSite(
                null, tmp.resolve("cf").toFile(), tmp.resolve("afu").toFile());
        assertEquals("old", read(page));
    }

    private static String asset(Path zip, String updatedAt) {
        return "{\"name\": \""
                + zip.getFileName()
                + "\", \"browser_download_url\": \""
                + zip.toUri()
                + "\", \"updated_at\": \""
                + updatedAt
                + "\"}";
    }

    private static void addEntry(ZipOutputStream out, String name, String content)
            throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(content.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }

    private static JSONObject parse(String json) throws ParseException {
        return (JSONObject) new JSONParser().parse(json);
    }

    private static Path write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
