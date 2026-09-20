/* (C)2023 */
package io.github.eisop.website;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class EisopSiteGenerator {

    public static void main(String[] args) throws IOException {
        File localReleaseZip = null;
        boolean onlyLatest = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--only-latest")) {
                onlyLatest = true;
            } else if (arg.equals("--local-release")) {
                if (i + 1 < args.length) {
                    localReleaseZip = new File(args[++i]);
                } else {
                    System.err.println(
                            "Error: --local-release requires a path to a release zip file");
                    System.exit(1);
                }
            } else if (arg.startsWith("--local-release=")) {
                localReleaseZip = new File(arg.substring("--local-release=".length()));
            } else {
                System.err.println("Unknown argument: " + arg);
                System.exit(1);
            }
        }

        if (localReleaseZip != null) {
            localReleaseZip = localReleaseZip.getAbsoluteFile();
            if (!localReleaseZip.isFile()) {
                System.err.println("Error: local release zip does not exist: " + localReleaseZip);
                System.exit(1);
            }
        }

        File directoryPath = new File(System.getProperty("user.dir") + "/cf");
        if (!directoryPath.exists()) {
            if (directoryPath.mkdirs()) {
                System.out.println("Folder created successfully at: " + directoryPath);
            } else {
                System.out.println("Failed to create folder at: " + directoryPath);
            }
        } else {
            System.out.println("Folder already exists at: " + directoryPath);
        }

        // Get list of framework releases
        URL listReleasesURL =
                URI.create(
                                "https://api.github.com/repos/eisop/checker-framework/releases?per_page=100")
                        .toURL();
        JSONArray frameworkReleases;
        try {
            frameworkReleases = getAPIResponse(listReleasesURL);
        } catch (IOException e) {
            if (localReleaseZip != null && onlyLatest) {
                System.out.println(
                        "Warning: could not reach GitHub API, proceeding with local release only: "
                                + e.getMessage());
                frameworkReleases = new JSONArray();
            } else {
                throw e;
            }
        }

        if (localReleaseZip != null) {
            String zipName = localReleaseZip.getName();
            if (!zipName.endsWith(".zip")) {
                System.err.println("Error: local release zip must end with .zip: " + zipName);
                System.exit(1);
            }
            String tagName = zipName.substring(0, zipName.length() - 4);
            Date lastModified = new Date(localReleaseZip.lastModified());
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String dateStr = isoFormat.format(lastModified);

            JSONObject localRelease = new JSONObject();
            localRelease.put("tag_name", tagName);
            JSONObject asset = new JSONObject();
            asset.put("name", zipName);
            asset.put("browser_download_url", localReleaseZip.toURI().toString());
            asset.put("created_at", dateStr);
            JSONArray assets = new JSONArray();
            assets.add(asset);
            localRelease.put("assets", assets);

            frameworkReleases.add(0, localRelease);
        }

        File releaseFile = new File(directoryPath, "releases/releases.md");
        String releaseFileHTML =
                "---\n"
                        + "layout: default\n"
                        + "title: Releases\n"
                        + "---\n"
                        + "![Checker Framework logo](../CFLogo.png)\n"
                        + "Previous Checker Framework Releases\n"
                        + "=====================\n";

        // Loop through list of framework releases
        int limit = onlyLatest ? 1 : frameworkReleases.size();
        for (int i = 0; i < limit; i++) {
            boolean isLocalRelease = (i == 0 && localReleaseZip != null);

            if (!isLocalRelease) {
                releaseFileHTML +=
                        "["
                                + String.valueOf(
                                        ((JSONObject) frameworkReleases.get(i)).get("tag_name"))
                                + "](../"
                                + String.valueOf(
                                        ((JSONObject) frameworkReleases.get(i)).get("tag_name"))
                                + "/index.html)\n";
            }

            // Get data on release assets
            JSONObject latestAssetsData =
                    (JSONObject)
                            ((((JSONArray) ((JSONObject) frameworkReleases.get(i)).get("assets"))
                                    .get(0)));
            int connectTimeout = 0;
            int readTimeout = 0;
            String fileUrlString = String.valueOf(latestAssetsData.get("browser_download_url"));
            URL fileUrl = URI.create(fileUrlString).toURL();
            File fileTest = new File(String.valueOf(latestAssetsData.get("name")));

            System.out.println("Checking release " + fileTest.getName());

            if (isLocalRelease) {
                // For a local release, clean up any previous copy and copy the local zip into place
                File existingZip = new File(directoryPath, fileTest.getName());
                if (existingZip.exists()) {
                    FileUtils.forceDelete(existingZip);
                }
                String folderName =
                        fileTest.getName()
                                .substring(0, fileTest.getName().length() - 4);
                File existingFolder = new File(directoryPath, folderName);
                if (existingFolder.exists()) {
                    FileUtils.deleteDirectory(existingFolder);
                }
                FileUtils.copyFile(localReleaseZip, fileTest);
            } else {
                // Check if we've already downloaded this release
                String[] contents = directoryPath.list();
                boolean alreadyDownloaded = false;
                if (contents != null) {
                    for (String item : contents) {
                        if (item.equals(fileTest.getName())) {
                            alreadyDownloaded = true;
                            System.out.println(
                                    "Release " + fileTest.getName() + " already downloaded\n");
                            break;
                        }
                    }
                }
                if (alreadyDownloaded) {
                    continue;
                }

                // If not already downloaded, download release assets
                FileUtils.copyURLToFile(fileUrl, fileTest, connectTimeout, readTimeout);
                System.out.println("Downloading " + fileTest.getName() + "\n");
            }

            // Unzip downloaded assets, move them to /cf
            String assetName = String.valueOf(latestAssetsData.get("name"));
            String assetBaseName = assetName.substring(0, assetName.length() - 4);
            File unzippedFile = new File(assetBaseName);
            try {
                ZipFile zipFile = new ZipFile(fileTest);
                if (zipFile.isEncrypted()) {
                    throw new RuntimeException(
                            "Encountered an encrypted zip file, which was not expected.");
                }
                zipFile.extractAll(assetBaseName);
            } catch (ZipException e) {
                e.printStackTrace();
            }

            FileUtils.moveFileToDirectory(fileTest, directoryPath, false);
            FileUtils.moveDirectoryToDirectory(unzippedFile, directoryPath, false);

            // Remove assets folder from enclosing folder
            File copyFolder = new File(directoryPath, unzippedFile.getName());
            File copyFolderRename = new File(directoryPath, unzippedFile.getName() + "_copy");
            copyFolder.renameTo(copyFolderRename);
            File innerFolder = new File(copyFolderRename, unzippedFile.getName());
            FileUtils.moveDirectoryToDirectory(innerFolder, directoryPath, false);
            FileUtils.deleteDirectory(copyFolderRename);

            File releaseFolder = new File(directoryPath, unzippedFile.getName());
            String releaseZipName = releaseFolder.getName() + ".zip";

            // Move javadoc.jar to /api and unzip for latest release, or write stub for older releases
            File releaseJavadoc = new File(releaseFolder, "checker/dist/checker-javadoc.jar");
            File javadocFolder = new File(releaseFolder, "api");
            FileUtils.forceMkdir(javadocFolder);

            if (i == 0) {
                // Keep full javadoc only for the latest release
                if (releaseJavadoc.exists()) {
                    FileUtils.moveFileToDirectory(releaseJavadoc, javadocFolder, false);
                    File unzippedJavadoc = new File(javadocFolder, "checker-javadoc.jar");
                    try {
                        ZipFile zipFile = new ZipFile(unzippedJavadoc);
                        zipFile.extractAll(
                                new File(javadocFolder, "checker-javadoc").getAbsolutePath());
                    } catch (ZipException e) {
                        e.printStackTrace();
                    }
                    FileUtils.forceDelete(unzippedJavadoc);
                }
            } else {
                // For older releases, write a stub redirecting to the release zip
                File stubDir = new File(javadocFolder, "checker-javadoc");
                FileUtils.forceMkdir(stubDir);
                File stubIndex = new File(stubDir, "index.html");
                String stubContent =
                        "---\n"
                                + "layout: default\n"
                                + "title: Javadoc\n"
                                + "---\n"
                                + "<p>Javadoc for this archived release is available in <a href=\"/cf/"
                                + releaseZipName
                                + "\">the release zip</a>.</p>\n";
                FileUtils.writeStringToFile(stubIndex, stubContent, StandardCharsets.UTF_8);
            }

            // Find the subdirectories and files to move
            File examplesDirectory = new File(releaseFolder, "docs/examples");
            File manualDirectory = new File(releaseFolder, "docs/manual");
            File tutorialDirectory = new File(releaseFolder, "docs/tutorial");
            File changelogFile = new File(releaseFolder, "docs/CHANGELOG.md");
            File quickStartFile = new File(releaseFolder, "docs/checker-framework-quick-start.html");
            File shippedWebpage = new File(releaseFolder, "docs/checker-framework-webpage.html");
            File shippedFavicon = new File(releaseFolder, "docs/favicon-checkerframework.png");
            File logoFile = new File(releaseFolder, "tutorial/CFLogo.png");

            // Move the subdirectories and files to the root of the directory
            if (examplesDirectory.exists()) {
                FileUtils.moveDirectoryToDirectory(examplesDirectory, releaseFolder, true);
            }
            if (manualDirectory.exists()) {
                FileUtils.moveDirectoryToDirectory(manualDirectory, releaseFolder, true);
            }
            if (tutorialDirectory.exists()) {
                FileUtils.moveDirectoryToDirectory(tutorialDirectory, releaseFolder, true);
            }
            if (changelogFile.exists()) {
                FileUtils.moveFileToDirectory(changelogFile, releaseFolder, true);
            }
            // Releases before the quick-start guide was added to the distribution do not have it.
            if (quickStartFile.exists()) {
                FileUtils.moveFile(quickStartFile, new File(releaseFolder, "quick-start.html"));
            }
            if (logoFile.exists()) {
                FileUtils.copyFileToDirectory(logoFile, releaseFolder);
            }

            // Generate release front page: use shipped page if present, else fall back to template
            File indexHTML = new File(releaseFolder, "index.html");
            File indexMD = new File(releaseFolder, "index.md");
            if (indexHTML.exists()) {
                FileUtils.forceDelete(indexHTML);
            }
            if (indexMD.exists()) {
                FileUtils.forceDelete(indexMD);
            }

            if (shippedWebpage.exists()) {
                String rawHtml = FileUtils.readFileToString(shippedWebpage, StandardCharsets.UTF_8);
                String themedHtml = themeShippedPage(rawHtml);
                FileUtils.writeStringToFile(indexHTML, themedHtml, StandardCharsets.UTF_8);
                if (shippedFavicon.exists()) {
                    FileUtils.moveFile(shippedFavicon, new File(releaseFolder, "favicon-checkerframework.png"));
                }
            } else {
                String htmlString = readTemplate("cf-template.md");
                htmlString =
                        htmlString.replace(
                                "$LatestCheckerFrameworkReleaseZip", releaseZipName);

                String latestCheckerFrameworkReleaseDownloadLink =
                        "/cf/" + releaseZipName;
                htmlString =
                        htmlString.replace(
                                "$LatestCheckerFrameworkReleaseDownloadLink",
                                latestCheckerFrameworkReleaseDownloadLink);

                String latestCheckerFrameworkReleaseDate =
                        String.valueOf(
                                ((JSONObject)
                                                ((((JSONArray)
                                                                ((JSONObject) frameworkReleases.get(i))
                                                                        .get("assets"))
                                                        .get(0))))
                                        .get("created_at"));
                latestCheckerFrameworkReleaseDate = getReadableDate(latestCheckerFrameworkReleaseDate);
                htmlString =
                        htmlString.replace(
                                "$LatestCheckerFrameworkReleaseDate",
                                latestCheckerFrameworkReleaseDate);

                FileUtils.writeStringToFile(indexMD, htmlString, StandardCharsets.UTF_8);
            }

            // Remove checker/ and docs/ to avoid keeping redundant build jars and files
            File checkerDir = new File(releaseFolder, "checker");
            if (checkerDir.exists()) {
                FileUtils.deleteDirectory(checkerDir);
            }
            File docsDir = new File(releaseFolder, "docs");
            if (docsDir.exists()) {
                FileUtils.deleteDirectory(docsDir);
            }
        }

        // Write HTML to releases/releases.md if not in only-latest mode
        if (!onlyLatest) {
            FileUtils.writeStringToFile(releaseFile, releaseFileHTML, StandardCharsets.UTF_8);
        }

        // Re-generate cf/index.html or cf/index.md with latest release
        File globalIndexHTML = new File(directoryPath, "index.html");
        if (globalIndexHTML.exists()) {
            FileUtils.forceDelete(globalIndexHTML);
        }
        File globalIndexMD = new File(directoryPath, "index.md");
        if (globalIndexMD.exists()) {
            FileUtils.forceDelete(globalIndexMD);
        }

        File latestRelease =
                new File(
                        directoryPath,
                        String.valueOf(
                                ((JSONObject) frameworkReleases.get(0)).get("tag_name")));
        File latestReleaseHTML = new File(latestRelease, "index.html");
        File latestReleaseMD = new File(latestRelease, "index.md");
        if (latestReleaseHTML.exists()) {
            FileUtils.copyFileToDirectory(latestReleaseHTML, directoryPath);
        } else if (latestReleaseMD.exists()) {
            FileUtils.copyFileToDirectory(latestReleaseMD, directoryPath);
        }

        File latestFavicon = new File(latestRelease, "favicon-checkerframework.png");
        if (latestFavicon.exists()) {
            FileUtils.copyFileToDirectory(latestFavicon, directoryPath);
        }

        // Get folders from latest release
        File newExamples = new File(directoryPath, "examples");
        if (newExamples.exists()) {
            FileUtils.forceDelete(newExamples);
        }
        File newManual = new File(directoryPath, "manual");
        if (newManual.exists()) {
            FileUtils.forceDelete(newManual);
        }
        File newTutorial = new File(directoryPath, "tutorial");
        if (newTutorial.exists()) {
            FileUtils.forceDelete(newTutorial);
        }
        File newChangelog = new File(directoryPath, "CHANGELOG.md");
        if (newChangelog.exists()) {
            FileUtils.forceDelete(newChangelog);
        }
        File newQuickStart = new File(directoryPath, "quick-start.html");
        if (newQuickStart.exists()) {
            FileUtils.forceDelete(newQuickStart);
        }
        File newJavadoc = new File(directoryPath, "api");
        if (newJavadoc.exists()) {
            FileUtils.forceDelete(newJavadoc);
        }

        File latestExamples = new File(latestRelease, "examples");
        File latestManual = new File(latestRelease, "manual");
        File latestTutorial = new File(latestRelease, "tutorial");
        File latestChangelog = new File(latestRelease, "CHANGELOG.md");
        File latestJavadoc = new File(latestRelease, "api");

        if (latestExamples.exists()) {
            FileUtils.copyDirectory(latestExamples, newExamples);
        }
        if (latestManual.exists()) {
            FileUtils.copyDirectory(latestManual, newManual);
        }
        if (latestTutorial.exists()) {
            FileUtils.copyDirectory(latestTutorial, newTutorial);
        }
        if (latestChangelog.exists()) {
            FileUtils.copyFile(latestChangelog, newChangelog);
        }
        File latestQuickStart = new File(latestRelease, "quick-start.html");
        if (latestQuickStart.exists()) {
            FileUtils.copyFile(latestQuickStart, newQuickStart);
        }
        if (latestJavadoc.exists()) {
            FileUtils.copyDirectory(latestJavadoc, newJavadoc);
        }

        System.out.println("Latest release: " + latestRelease);

        // Rename cf/manual/manual.pdf to cf/manual/checker-framework-manual.pdf
        File manualPDF = new File(directoryPath, "manual/manual.pdf");
        if (manualPDF.exists()) {
            File checkerFrameworkManualPDF =
                    new File(directoryPath, "manual/checker-framework-manual.pdf");
            FileUtils.copyFile(manualPDF, checkerFrameworkManualPDF);
        }

        // Copy CFLogo.png to cf/
        File cfLogo = new File(latestRelease, "CFLogo.png");
        if (!cfLogo.exists()) {
            cfLogo = new File(latestRelease, "tutorial/CFLogo.png");
        }
        if (cfLogo.exists()) {
            File newCFLogo = new File(directoryPath, "CFLogo.png");
            FileUtils.copyFile(cfLogo, newCFLogo);
        }

        getAFU(onlyLatest);
    }

    /**
     * Fetches the given API URL and parses the response as a JSON array.
     *
     * @param apiURL the URL to fetch
     * @return the parsed response
     * @throws IOException if the request fails or the response cannot be parsed
     */
    static JSONArray getAPIResponse(URL apiURL) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) apiURL.openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "eisop-website-generator");
            String token = System.getenv("GITHUB_TOKEN");
            if (token != null && !token.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token.trim());
            }
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorDetails = "";
                try (InputStream err = conn.getErrorStream()) {
                    if (err != null) {
                        errorDetails = ": " + new String(err.readAllBytes(), StandardCharsets.UTF_8);
                    }
                } catch (IOException ignored) {
                }
                throw new IOException("GET " + apiURL + " returned HTTP " + responseCode + errorDetails);
            }

            String responseBody;
            try (InputStream in = conn.getInputStream()) {
                responseBody = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            JSONParser parse = new JSONParser();
            return (JSONArray) parse.parse(responseBody);
        } catch (ParseException e) {
            throw new IOException("Could not parse the response from " + apiURL, e);
        } finally {
            conn.disconnect();
        }
    }

    static void getAFU(boolean onlyLatest) throws IOException {
        File directoryPath = new File(System.getProperty("user.dir") + "/afu");
        if (!directoryPath.exists()) {
            if (directoryPath.mkdirs()) {
                System.out.println("Folder created successfully at: " + directoryPath);
            } else {
                System.out.println("Failed to create folder at: " + directoryPath);
            }
        } else {
            System.out.println("Folder already exists at: " + directoryPath);
        }

        // Get list of framework releases
        URL listReleasesURL =
                URI.create(
                                "https://api.github.com/repos/eisop/annotation-tools/releases?per_page=100")
                        .toURL();
        JSONArray frameworkReleases = getAPIResponse(listReleasesURL);

        // Loop through list of framework releases
        int limit = onlyLatest ? 1 : frameworkReleases.size();
        for (int i = 0; i < limit; i++) {
            // Get data on release assets
            JSONObject latestAssetsData =
                    (JSONObject)
                            ((((JSONArray) ((JSONObject) frameworkReleases.get(i)).get("assets"))
                                    .get(0)));
            int connectTimeout = 0;
            int readTimeout = 0;
            String fileUrlString = String.valueOf(latestAssetsData.get("browser_download_url"));
            URL fileUrl = URI.create(fileUrlString).toURL();
            File fileTest = new File(String.valueOf(latestAssetsData.get("name")));

            System.out.println("Checking release " + fileTest.getName());

            // Check if we've already downloaded this release
            String[] contents = directoryPath.list();
            boolean alreadyDownloaded = false;
            if (contents != null) {
                for (String item : contents) {
                    if (item.equals(fileTest.getName())) {
                        alreadyDownloaded = true;
                        System.out.println(
                                "Release " + fileTest.getName() + " already downloaded\n");
                        break;
                    }
                }
            }
            if (alreadyDownloaded) {
                continue;
            }

            // If not already downloaded, download release assets
            FileUtils.copyURLToFile(fileUrl, fileTest, connectTimeout, readTimeout);
            System.out.println("Downloading " + fileTest.getName() + "\n");

            // Move the zip to afu/ without extracting the source tree
            FileUtils.moveFileToDirectory(fileTest, directoryPath, false);
        }

        // Re-generate afu/annotation-file-utilities.html with latest release
        File globalIndexHTML =
                new File(directoryPath, "annotation-file-utilities.html");
        if (globalIndexHTML.exists()) {
            FileUtils.forceDelete(globalIndexHTML);
        }
        String latestReleaseZipName =
                String.valueOf(
                        ((JSONObject)
                                        ((JSONArray) ((JSONObject) frameworkReleases.get(0))
                                                        .get("assets"))
                                                .get(0))
                                .get("name"));
        String latestReleaseName =
                latestReleaseZipName.substring(0, latestReleaseZipName.length() - 4);

        // Extract format documentation from the latest AFU release zip if not already present
        File latestFormatHTML = new File(directoryPath, "annotation-file-format.html");
        File latestFormatPDF = new File(directoryPath, "annotation-file-format.pdf");
        if (!latestFormatHTML.exists() || !latestFormatPDF.exists()) {
            File latestZip = new File(directoryPath, latestReleaseZipName);
            if (latestZip.exists()) {
                File tempDir = File.createTempFile("afu-extract", "");
                tempDir.delete();
                tempDir.mkdirs();
                try {
                    ZipFile zipFile = new ZipFile(latestZip);
                    zipFile.extractAll(tempDir.getAbsolutePath());
                    File formatHTML =
                            new File(
                                    tempDir,
                                    latestReleaseName
                                            + "/annotation-file-utilities/annotation-file-format.html");
                    if (formatHTML.exists()) {
                        FileUtils.copyFileToDirectory(formatHTML, directoryPath);
                    }
                    File formatPDF =
                            new File(
                                    tempDir,
                                    latestReleaseName
                                            + "/annotation-file-utilities/annotation-file-format.pdf");
                    if (formatPDF.exists()) {
                        FileUtils.copyFileToDirectory(formatPDF, directoryPath);
                    }
                } catch (ZipException e) {
                    e.printStackTrace();
                } finally {
                    FileUtils.deleteQuietly(tempDir);
                }
            }
        }

        System.out.println("Latest release: " + latestReleaseName);

        String latestAnnotationFileUtilitiesReleaseZip = latestReleaseName + ".zip";

        String latestAnnotationFileUtilitiesReleaseDownloadLink =
                "/afu/" + latestAnnotationFileUtilitiesReleaseZip;

        String latestAnnotationFileUtilitiesReleaseDate =
                String.valueOf(
                        ((JSONObject)
                                        ((((JSONArray)
                                                        ((JSONObject) frameworkReleases.get(0))
                                                                .get("assets"))
                                                .get(0))))
                                .get("created_at"));
        latestAnnotationFileUtilitiesReleaseDate =
                getReadableDate(latestAnnotationFileUtilitiesReleaseDate);

        substituteAfuPlaceholders(
                new File(System.getProperty("user.dir"), "cf"),
                latestAnnotationFileUtilitiesReleaseZip,
                latestAnnotationFileUtilitiesReleaseDownloadLink,
                latestAnnotationFileUtilitiesReleaseDate);

        // Re-generate afu/annotation-file-utilities.md with latest release
        File newMD = new File(directoryPath, "annotation-file-utilities.md");
        String mdString = readTemplate("afu-template.md");

        String[] parts = latestReleaseName.split("-");
        String latestAnnotationFileUtilitiesRelease = parts[parts.length - 2];

        mdString =
                mdString.replace(
                        "$LatestAnnotationFileUtilitiesReleaseDate",
                        latestAnnotationFileUtilitiesReleaseDate);
        mdString =
                mdString.replace(
                        "$LatestAnnotationFileUtilitiesReleaseZip",
                        latestAnnotationFileUtilitiesReleaseZip);
        mdString =
                mdString.replace(
                        "$LatestAnnotationFileUtilitiesReleaseDownloadLink",
                        latestAnnotationFileUtilitiesReleaseDownloadLink);
        mdString =
                mdString.replace(
                        "$LatestAnnotationFileUtilitiesRelease",
                        latestAnnotationFileUtilitiesRelease);

        if (newMD.exists()) {
            FileUtils.forceDelete(newMD);
        }
        newMD.createNewFile();
        FileUtils.writeStringToFile(newMD, mdString, StandardCharsets.UTF_8);
    }

    /**
     * Fills in the Annotation File Utilities placeholders on every page generated from
     * cf-template.md.
     *
     * <p>That template is expanded in two passes: main() substitutes the Checker Framework values
     * as it writes each release's page, and the AFU values are known only later, once the AFU
     * releases have been read. This pass therefore has to reach every page the first pass wrote --
     * the per-release pages, not just cf/index.md -- or those pages show readers the literal
     * placeholder text.
     *
     * <p>Substitution is idempotent, so pages written by an earlier run are repaired too.
     *
     * @param cfDir the website's cf directory
     * @param zip the file name of the latest AFU release's zip
     * @param downloadLink the site-relative download link for that zip
     * @param date the human-readable date of that release
     * @throws IOException if a page cannot be read or written
     */
    static void substituteAfuPlaceholders(File cfDir, String zip, String downloadLink, String date)
            throws IOException {
        List<File> pages = new ArrayList<>();
        File cfIndexMd = new File(cfDir, "index.md");
        if (cfIndexMd.exists()) {
            pages.add(cfIndexMd);
        }
        File[] releaseDirs = cfDir.listFiles(File::isDirectory);
        if (releaseDirs != null) {
            for (File releaseDir : releaseDirs) {
                File page = new File(releaseDir, "index.md");
                if (page.exists()) {
                    pages.add(page);
                }
            }
        }

        for (File page : pages) {
            if (!page.isFile()) {
                continue;
            }
            String content = FileUtils.readFileToString(page, StandardCharsets.UTF_8);
            String substituted =
                    content.replace("$LatestAnnotationFileUtilitiesReleaseZip", zip)
                            .replace(
                                    "$LatestAnnotationFileUtilitiesReleaseDownloadLink",
                                    downloadLink)
                            .replace("$LatestAnnotationFileUtilitiesReleaseDate", date);
            if (!substituted.equals(content)) {
                FileUtils.writeStringToFile(page, substituted, StandardCharsets.UTF_8);
                System.out.println("Filled in AFU release info in " + page);
            }
        }
    }

    /**
     * Converts a standalone HTML page shipped in a release zip into a Jekyll-compatible page
     * using the default layout.
     *
     * @param html the full HTML content
     * @return the body content with Jekyll front matter prepended
     */
    static String themeShippedPage(String html) {
        String lower = html.toLowerCase();
        int bodyStart = lower.indexOf("<body");
        String body = html;
        if (bodyStart != -1) {
            int bodyStartClose = html.indexOf('>', bodyStart);
            if (bodyStartClose != -1) {
                body = html.substring(bodyStartClose + 1);
                lower = lower.substring(bodyStartClose + 1);
            }
        }
        int bodyEnd = lower.indexOf("</body>");
        if (bodyEnd != -1) {
            body = body.substring(0, bodyEnd);
        }
        return "---\nlayout: default\n---\n" + body.trim() + "\n";
    }

    /**
     * Reads a page template packaged in this tool's own jar.
     *
     * <p>The templates are classpath resources rather than files in the working directory because
     * the generator is run from a gh-pages checkout, which carries its own copies of the source
     * files. Reading them from disk there used whichever template that branch happened to hold,
     * not the one built alongside this code, so editing a template on master had no effect until
     * someone remembered to copy it across. Being resources also keeps them out of Jekyll's input,
     * which was publishing them as /cf-template.html and /afu-template.html with their
     * placeholders unfilled.
     *
     * @param name the resource file name, such as "cf-template.md"
     * @return the contents of that template
     * @throws IOException if the resource is missing or cannot be read
     */
    static String readTemplate(String name) throws IOException {
        String path = name.startsWith("/") ? name : "/" + name;
        try (InputStream in = EisopSiteGenerator.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Template is not on the classpath: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static String getReadableDate(String autoDate) {
        String[] splitDate = autoDate.split("-", 0);
        String[] months = {
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December"
        };

        String year = splitDate[0];
        String month = splitDate[1];
        month = months[Integer.parseInt(month) - 1];
        String day = splitDate[2].substring(0, 2);

        return month + " " + day + ", " + year;
    }
}
