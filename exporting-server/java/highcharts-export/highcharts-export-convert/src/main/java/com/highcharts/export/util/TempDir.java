/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.highcharts.export.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author gert
 */
public class TempDir {
  protected static Logger logger = Logger.getLogger("files");

  public static final String EXPORT_DIR = "export";
  public static final String OUTPUT_DIR = "output";
  public static final String PHANTOMJS_DIR = "phantomjs";
  public static final String DOWNLOAD_DIR = "files";
  public static final String DATE_FORMAT = "yyyyMMdd";

	@Value("${chart.file.base.dir:/var/local/highcharts}")
  private String baseDir;
  @Value("${chart.file.num.nested.dir:1}")
  private String numberOfNestedDirectories;

  private Path tmpDir;
	private Path outputDir;
	private Path phantomJsDir;

	@PostConstruct
  public void init() throws IOException {
		tmpDir = ensureDirectory(Paths.get(baseDir, EXPORT_DIR));
    outputDir = ensureDirectory(Paths.get(tmpDir.toString(), OUTPUT_DIR));
    phantomJsDir = ensureDirectory(Paths.get(tmpDir.toString(), PHANTOMJS_DIR));

    System.out.println("Highcharts Export Server is using " + getTmpDir() + " as file folder.");
	}

  private Path ensureDirectory(Path path) throws IOException {
    if ( ! path.toFile().exists()) {
      path = Files.createDirectory(path);
    }
    return path;
  }

	public Path getTmpDir() {
		return tmpDir;
	}

	public Path getOutputDir() {
		return outputDir;
	}

	public Path getPhantomJsDir() {
		return phantomJsDir;
	}

	public String getDownloadLink(String filename) {
		filename = FilenameUtils.getName(filename);
		return DOWNLOAD_DIR + "/" + filename;
	}

  public int getNumberOfNestedDirectories() {
    return Integer.valueOf(numberOfNestedDirectories);
  }

  public Path getFilePath(String filename) {
    String baseFilePath = getOutputDir().toString();
    for (int i = 0; i < getNumberOfNestedDirectories(); i++) {
      baseFilePath += "/" + filename.toCharArray()[i];
    }
    return Paths.get(baseFilePath, filename);
  }

  public String generateRandomFilename() {
    String name = UUID.randomUUID().toString().replaceAll("-","");
    return name + getDateString();
  }

  public static Date getDateFromFilename(String filename) {
    try {
      // strip off the extension
      if (filename.indexOf('.') >= 0) {
        filename = filename.substring(0, filename.lastIndexOf('.'));
      }
      if (filename.length() > DATE_FORMAT.length()) {
        String dateString = filename.substring(filename.length() - DATE_FORMAT.length());
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        return df.parse(dateString);
      }
    } catch (Exception ex) {
      logger.error("Unable to parse date from string: " + filename);
    }
    return null;
  }

  private static String getDateString() {
    DateFormat df = new SimpleDateFormat(DATE_FORMAT);
    Date today = Calendar.getInstance().getTime();
    return df.format(today);
  }
}
