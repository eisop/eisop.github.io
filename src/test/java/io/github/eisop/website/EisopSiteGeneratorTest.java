/* (C)2026 */
package io.github.eisop.website;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
    void themeShippedPageKeepsTheVersionMarker() {
        // The release scripts of the Checker Framework read the current version from this marker.
        String marker =
                "<!-- checker-framework-zip-version -->checker-framework-1.2.3.zip"
                        + "<!-- /checker-framework-zip-version -->";
        String themed =
                EisopSiteGenerator.themeShippedPage(
                        "<html><body><a href=\"/cf/x.zip\">" + marker + "</a></body></html>");
        assertTrue(themed.contains(marker), themed);
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

    private static Path write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
