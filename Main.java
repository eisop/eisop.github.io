package org.example;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main {

    public static void main(String[] args) throws IOException {

        File directoryPath = new File(System.getProperty("user.dir") + "/cf");

        // Get list of framework releases
        URL listReleasesURL = new URL("https://api.github.com/repos/eisop/checker-framework/releases");
        JSONArray frameworkReleases = getAPIResponse(listReleasesURL, "GET");

        File releaseFile = new File(System.getProperty("user.dir") + "/cf/releases/releases.md");
        String releaseFileHTML = "---\n" +
                "layout: default\n" +
                "title: Releases\n" +
                "---\n" +
                "![Checker Framework logo](../CFLogo.png)\n" +
                "Previous Checker Framework Releases\n" +
                "=====================\n";



        // Loop through list of framework releases
        for (int i = 0; i < frameworkReleases.size(); i++) {

            JSONObject LatestReleaseData = (JSONObject) frameworkReleases.get(i);
            releaseFileHTML += "[" + String.valueOf(((JSONObject) frameworkReleases.get(i)).get("tag_name")) + "](../" + String.valueOf(((JSONObject) frameworkReleases.get(i)).get("tag_name")) + "/index.html)\n";

            // Get data on release assets
            JSONObject LatestAssetsData = (JSONObject)((((JSONArray)((JSONObject) frameworkReleases.get(i)).get("assets")).get(0)));
            int CONNECT_TIMEOUT = 0;
            int READ_TIMEOUT = 0;
            String assetID = (String.valueOf(LatestAssetsData.get("id")));
            String FILE_URL_STRING = (String.valueOf(LatestAssetsData.get("browser_download_url")));
            URL FILE_URL = new URL(FILE_URL_STRING);
            File FILE_TEST = new File(String.valueOf(LatestAssetsData.get("name")));

            System.out.println("Checking release " + String.valueOf(FILE_TEST));


            // Check if we've already downloaded this release
            String contents[] = directoryPath.list();
            boolean alreadyDownloaded = false;
            if(contents != null) {
                for(int j = 0; j < contents.length; j++){
                    //System.out.println(contents[j]);
                    if(String.valueOf(contents[j]).equals(String.valueOf(FILE_TEST))){
                        alreadyDownloaded = true;
                        System.out.println("Release " + String.valueOf(FILE_TEST) + " already downloaded");
                        System.out.println("");
                        break;
                    }
                }
            }
            if(alreadyDownloaded){
                continue;
            }

            // If not already downloaded, download release assets
            FileUtils.copyURLToFile(FILE_URL, FILE_TEST, CONNECT_TIMEOUT, READ_TIMEOUT);
            System.out.println("Downloading " + String.valueOf(FILE_TEST));
            System.out.println("");

            // Unzip downloaded assets, move them to /cf
            File unzippedFile = new File(String.valueOf(LatestAssetsData.get("name")).substring(0, String.valueOf(LatestAssetsData.get("name")).length() - 4));
            try {
                ZipFile zipFile = new ZipFile(FILE_TEST);
                if (zipFile.isEncrypted()) {
                    //zipFile.setPassword();
                }
                zipFile.extractAll(String.valueOf(LatestAssetsData.get("name")).substring(0, String.valueOf(LatestAssetsData.get("name")).length() - 4));
            } catch (ZipException e) {
                e.printStackTrace();
            }
            FileUtils.moveFileToDirectory(FILE_TEST, directoryPath, false);
            FileUtils.moveDirectoryToDirectory(unzippedFile, directoryPath, false);

            // Remove assets folder from enclosing folder
            File copyFolder = new File(System.getProperty("user.dir") + "/cf/" + String.valueOf(unzippedFile));
            File copyFolderRename = new File(System.getProperty("user.dir") + "/cf/" + String.valueOf(unzippedFile) + "_copy");
            copyFolder.renameTo(copyFolderRename);
            File innerFolder = new File(String.valueOf(copyFolderRename) + "/" + String.valueOf(unzippedFile));
            FileUtils.moveDirectoryToDirectory(innerFolder, directoryPath, false);
            FileUtils.deleteDirectory(copyFolderRename);

            File releaseFolder = new File(String.valueOf(directoryPath) + "/" + String.valueOf(unzippedFile));
            //System.out.println(String.valueOf(releaseFolder));

            // Move javadoc.jar to /api and unzip
            File releaseJavadoc = new File(String.valueOf(releaseFolder) + "/checker/dist/checker-javadoc.jar");
            File javadocFolder = new File(String.valueOf(releaseFolder) + "/api");
            FileUtils.forceMkdir(javadocFolder);
            FileUtils.moveFileToDirectory(releaseJavadoc, javadocFolder, false);

            File unzippedJavadoc = new File(String.valueOf(releaseFolder) + "/api/checker-javadoc.jar");
            try {
                ZipFile zipFile = new ZipFile(unzippedJavadoc);
                if (zipFile.isEncrypted()) {
                    //zipFile.setPassword();
                }
                zipFile.extractAll(String.valueOf(releaseFolder) + "/api/checker-javadoc");
            } catch (ZipException e) {
                e.printStackTrace();
            }

            // Move index.html
//            File releaseHTML = new File(String.valueOf(releaseFolder) + "/docs/tutorial/index.html");
//            FileUtils.moveFileToDirectory(releaseHTML, releaseFolder, false);

            File newHTML = new File(releaseFolder + "/index.html");
            if(newHTML.exists()){
                FileUtils.forceDelete(newHTML);
            }

            File htmlTemplateFile = new File(System.getProperty("user.dir") + "/template.html");
            String htmlString = FileUtils.readFileToString(htmlTemplateFile);
            //$LatestCheckerFrameworkReleaseDownloadLink, $LatestCheckerFrameworkReleaseZip, $LatestCheckerFrameworkReleaseDate

            String LatestCheckerFrameworkReleaseZip = String.valueOf(releaseFolder).split("/", 0)[String.valueOf(releaseFolder).split("/", 0).length-1] + ".zip";
            htmlString = htmlString.replace("$LatestCheckerFrameworkReleaseZip", LatestCheckerFrameworkReleaseZip);

            String LatestCheckerFrameworkReleaseDownloadLink = "/cf/" + LatestCheckerFrameworkReleaseZip;
            htmlString = htmlString.replace("$LatestCheckerFrameworkReleaseDownloadLink", LatestCheckerFrameworkReleaseDownloadLink);

            String LatestCheckerFrameworkReleaseDate = String.valueOf(((JSONObject)((((JSONArray)((JSONObject) frameworkReleases.get(i)).get("assets")).get(0)))).get("created_at"));
            LatestCheckerFrameworkReleaseDate = getReadableDate(LatestCheckerFrameworkReleaseDate);
            htmlString = htmlString.replace("$LatestCheckerFrameworkReleaseDate", LatestCheckerFrameworkReleaseDate);

            FileUtils.writeStringToFile(newHTML, htmlString);

            File releaseArchiveHTML = new File(System.getProperty("user.dir") + "/cf/releases/" + String.valueOf(releaseFolder).split("/", 0)[String.valueOf(releaseFolder).split("/", 0).length-1] + ".html");
            FileUtils.copyFile(newHTML, releaseArchiveHTML);

//            // Set the path to the directory to search
//            String directoryPath = "/path/to/directory";

            // Find the subdirectories and files to move
            String examplesString = String.valueOf(releaseFolder) + "/docs/examples";
            File examplesDirectory = new File(examplesString);
            String manualString = String.valueOf(releaseFolder) + "/docs/manual";
            File manualDirectory = new File(manualString);
            String tutorialString = String.valueOf(releaseFolder) + "/docs/tutorial";
            File tutorialDirectory = new File(tutorialString);
            String changeLogString = String.valueOf(releaseFolder) + "/docs/CHANGELOG.md";
            File changelogFile = new File(changeLogString);
            String logoString = String.valueOf(releaseFolder) + "/tutorial/CFLogo.png";
            File logoFile = new File(logoString);

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
            if (logoFile.exists()) {
                FileUtils.copyFileToDirectory(logoFile, releaseFolder);
            }

        }

        // Write HTML to releases/releases.md
        FileUtils.writeStringToFile(releaseFile, releaseFileHTML);

        // Re-generate cf/index.html with latest release
        File globalIndexHTML = new File(String.valueOf(directoryPath) + "/index.html");
        if(globalIndexHTML.exists()){
            FileUtils.forceDelete(globalIndexHTML);
        }

        File latestRelease = new File(String.valueOf(System.getProperty("user.dir")) + "/cf/" + String.valueOf(((JSONObject) frameworkReleases.get(0)).get("tag_name")));
        File latestReleaseHTML = new File(String.valueOf(latestRelease) + "/index.html");
        FileUtils.copyFileToDirectory(latestReleaseHTML, directoryPath);


        // Get folders from latest release
        File newExamples = new File(System.getProperty("user.dir") + "/cf/examples");
        if(newExamples.exists()){
            FileUtils.forceDelete(newExamples);
        }
        File newManual = new File(System.getProperty("user.dir") + "/cf/manual");
        if(newManual.exists()){
            FileUtils.forceDelete(newManual);
        }
        File newTutorial = new File(System.getProperty("user.dir") + "/cf/tutorial");
        if(newTutorial.exists()){
            FileUtils.forceDelete(newTutorial);
        }
        File newChangelog = new File(System.getProperty("user.dir") + "/cf/CHANGELOG.md");
        if(newChangelog.exists()){
            FileUtils.forceDelete(newChangelog);
        }
        File newJavadoc = new File(System.getProperty("user.dir") + "/cf/api");
        if(newJavadoc.exists()){
            FileUtils.forceDelete(newJavadoc);
        }

        File latestExamples = new File(String.valueOf(latestRelease) + "/examples");
        File latestManual = new File(String.valueOf(latestRelease) + "/manual");
        File latestTutorial = new File(String.valueOf(latestRelease) + "/tutorial");
        File latestChangelog = new File(String.valueOf(latestRelease) + "/CHANGELOG.md");
        File latestJavadoc = new File(String.valueOf(latestRelease) + "/api");


        FileUtils.copyDirectory(latestExamples, newExamples);
        FileUtils.copyDirectory(latestManual, newManual);
        FileUtils.copyDirectory(latestTutorial, newTutorial);
        FileUtils.copyFile(latestChangelog, newChangelog);
        FileUtils.copyDirectory(latestJavadoc, newJavadoc);


        System.out.println("Latest release: " + String.valueOf(latestRelease));

        getAFU();




    }

    static JSONArray getAPIResponse(URL APIURL, String RequestMethod) {
        try {
            //URL listReleasesURL = new URL("https://api.github.com/repos/eisop/checker-framework/releases");
            //URL listAssetURL = new URL("https://api.github.com/repos/eisop/checker-framework/releases/85376204/assets");

            HttpURLConnection conn = (HttpURLConnection) APIURL.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            //Check if connect is made
            int responseCode = conn.getResponseCode();
            conn.disconnect();

            // 200 OK
            if (responseCode != 200) {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            } else {

                StringBuilder informationString = new StringBuilder();
                Scanner scanner = new Scanner(APIURL.openStream());

                while (scanner.hasNext()) {
                    informationString.append(scanner.nextLine());
                }
                //Close the scanner
                scanner.close();

                //System.out.println(informationString);


                //JSON simple library Setup with Maven is used to convert strings to JSON
                JSONParser parse = new JSONParser();
                JSONArray dataObject = (JSONArray) parse.parse(String.valueOf(informationString));



                return dataObject;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static void getAFU() throws IOException {
        File directoryPath = new File(System.getProperty("user.dir") + "/afu");

        // Get list of framework releases
        URL listReleasesURL = new URL("https://api.github.com/repos/eisop/annotation-tools/releases");
        JSONArray frameworkReleases = getAPIResponse(listReleasesURL, "GET");

        // Loop through list of framework releases
        for (int i = 0; i < frameworkReleases.size(); i++) {

            JSONObject LatestReleaseData = (JSONObject) frameworkReleases.get(i);

            // Get data on release assets
            JSONObject LatestAssetsData = (JSONObject)((((JSONArray)((JSONObject) frameworkReleases.get(i)).get("assets")).get(0)));
            int CONNECT_TIMEOUT = 0;
            int READ_TIMEOUT = 0;
            String assetID = (String.valueOf(LatestAssetsData.get("id")));
            String FILE_URL_STRING = (String.valueOf(LatestAssetsData.get("browser_download_url")));
            URL FILE_URL = new URL(FILE_URL_STRING);
            File FILE_TEST = new File(String.valueOf(LatestAssetsData.get("name")));

            System.out.println("Checking release " + String.valueOf(FILE_TEST));


            // Check if we've already downloaded this release
            String contents[] = directoryPath.list();
            boolean alreadyDownloaded = false;
            for(int j = 0; j < contents.length; j++){
                //System.out.println(contents[j]);
                if(String.valueOf(contents[j]).equals(String.valueOf(FILE_TEST))){
                    alreadyDownloaded = true;
                    System.out.println("Release " + String.valueOf(FILE_TEST) + " already downloaded");
                    System.out.println("");
                    break;
                }
            }
            if(alreadyDownloaded){
                continue;
            }

            // If not already downloaded, download release assets
            FileUtils.copyURLToFile(FILE_URL, FILE_TEST, CONNECT_TIMEOUT, READ_TIMEOUT);
            System.out.println("Downloading " + String.valueOf(FILE_TEST));
            System.out.println("");

            // Unzip downloaded assets, move them to /cf
            File unzippedFile = new File(String.valueOf(LatestAssetsData.get("name")).substring(0, String.valueOf(LatestAssetsData.get("name")).length() - 4));
            try {
                ZipFile zipFile = new ZipFile(FILE_TEST);
                if (zipFile.isEncrypted()) {
                    //zipFile.setPassword();
                }
                zipFile.extractAll(String.valueOf(LatestAssetsData.get("name")).substring(0, String.valueOf(LatestAssetsData.get("name")).length() - 4));
            } catch (ZipException e) {
                e.printStackTrace();
            }
            FileUtils.moveFileToDirectory(FILE_TEST, directoryPath, false);
            FileUtils.moveDirectoryToDirectory(unzippedFile, directoryPath, false);


        }

        // Re-generate cf/annotation-file-utilities.html with latest release
        File globalIndexHTML = new File(String.valueOf(directoryPath) + "/annotation-file-utilities.html");
        if(globalIndexHTML.exists()){
            FileUtils.forceDelete(globalIndexHTML);
        }
        File[] files = directoryPath.listFiles();
        Arrays.sort(files);
        File latestRelease = new File(String.valueOf(files[files.length-1]).substring(0, String.valueOf(String.valueOf(files[files.length-1])).length() - 4));
        File latestReleaseHTML = new File(String.valueOf(latestRelease) + "/annotation-file-utilities/annotation-file-utilities.html");
        FileUtils.copyFileToDirectory(latestReleaseHTML, directoryPath);

        System.out.println("Latest release: " + String.valueOf(latestRelease));


        // Re-generate cf/index.html with latest release
        File newHTML = new File(System.getProperty("user.dir") + "/cf/index.html");

        String htmlString = FileUtils.readFileToString(newHTML);

        //$LatestAnnotationFileUtilitiesReleaseDownloadLink, $LatestAnnotationFileUtilitiesReleaseZip, $LatestAnnotationFileUtilitiesReleaseDate

        String LatestAnnotationFileUtilitiesReleaseZip = String.valueOf(latestRelease).split("/", 0)[String.valueOf(latestRelease).split("/", 0).length-1] + ".zip";
        htmlString = htmlString.replace("$LatestAnnotationFileUtilitiesReleaseZip", LatestAnnotationFileUtilitiesReleaseZip);

        String LatestAnnotationFileUtilitiesReleaseDownloadLink = "/afu/" + LatestAnnotationFileUtilitiesReleaseZip;
        htmlString = htmlString.replace("$LatestAnnotationFileUtilitiesReleaseDownloadLink", LatestAnnotationFileUtilitiesReleaseDownloadLink);

        String LatestAnnotationFileUtilitiesReleaseDate = String.valueOf(((JSONObject)((((JSONArray)((JSONObject) frameworkReleases.get(0)).get("assets")).get(0)))).get("created_at"));
        LatestAnnotationFileUtilitiesReleaseDate = getReadableDate(LatestAnnotationFileUtilitiesReleaseDate);
        htmlString = htmlString.replace("$LatestAnnotationFileUtilitiesReleaseDate", LatestAnnotationFileUtilitiesReleaseDate);

        if(newHTML.exists()){
            FileUtils.forceDelete(newHTML);
        }
        FileUtils.writeStringToFile(newHTML, htmlString);

    }

    static String getReadableDate(String autoDate){
        String[] splitDate = autoDate.split("-", 0);
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

        String year = splitDate[0];
        String month = splitDate[1];
        month = months[Integer.valueOf(month)-1];
        String day = splitDate[2].substring(0,2);

        return month + " " + day + ", " + year;
    }

}