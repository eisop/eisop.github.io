/* (C)2026 */
package io.github.eisop.website;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
