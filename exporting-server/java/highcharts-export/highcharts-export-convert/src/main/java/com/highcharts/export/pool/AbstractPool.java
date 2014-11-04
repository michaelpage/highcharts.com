package com.highcharts.export.pool;

import com.highcharts.export.util.TempDir;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractPool<T> implements ObjectPool<T> {

	final ObjectFactory<T> objectFactory;
	Queue<T> queue;
	final AtomicInteger poolSize = new AtomicInteger(0);
	int maxWait;
	final int capacity;
	final long retentionTime;
	protected static Logger logger = Logger.getLogger("pool");

  @Autowired
  protected TempDir tempDir;

	public AbstractPool(ObjectFactory<T> objectFactory, int number, int maxWait, Long retentionTime) throws PoolException {
		this.objectFactory = objectFactory;
		this.capacity = number;
		this.maxWait = maxWait;
		this.retentionTime = retentionTime;
	}

	@Override
	public void createObject() {
		T object = objectFactory.create();
		queue.add(object);
		poolSize.getAndIncrement();
	}

	@Override
	public void destroyObject(T object) {
		objectFactory.destroy(object);
	}

	@Override
	@Scheduled(initialDelay = 10000, fixedRate = 60000)
	public void poolCleaner() throws InterruptedException, PoolException {

		logger.debug("HC: queue size: " + queue.size() + " poolSize " + poolSize.get());

		int size = poolSize.get();
		// remove invalid objects
		for (int i = 0; i < size; i++) {
			T object = borrowObject();
			if (object == null) {
				logger.debug("HC: object is null");
				continue;
			} else {
				logger.debug("HC: validating " + object.toString());
				if (!objectFactory.validate(object)) {
					logger.debug("HC: destroying " + object.toString());
					destroyObject(object);
				} else {
					returnObject(object, false);
				}
			}
		}

		int number = poolSize.get() - capacity;
		logger.debug("in cleanpool, the surplus or shortage is: " + number);
		synchronized (this) {
				int iterations = Math.abs(number);
				for (int i = 0; i < iterations; i++) {
					if (number < 1) {
						this.createObject();
					} else {
						T object = borrowObject();
						this.destroyObject(object);
					}
				}
		}
	}

	// Clean generated files once a day.
  @Override
	@Scheduled(initialDelay = 15000, fixedRate = 86400000)
	public void tempDirCleaner() {
		IOFileFilter filter = new IOFileFilter() {

			@Override
			public boolean accept(File file) {
				try {
          if (file.isFile()) {
            Date today = Calendar.getInstance().getTime();
            Long now = today.getTime();
            Path path = Paths.get(file.getAbsolutePath());

            Date fileDate = TempDir.getDateFromFilename(path.toString());
            if (fileDate == null) {
              fileDate = today;
            }

            Long inBetween = now - fileDate.getTime();

            if (inBetween > retentionTime) {
              return true;
            }
          }
        } catch (Exception ex) {
					logger.error("Error: while selection files for deletion: "  + ex.getMessage());
				}
				return false;
			}

			@Override
			public boolean accept(File file, String string) {
				throw new UnsupportedOperationException("Not supported yet."); 
			}
		};

		// Find files matching the IOFileFilter (includes all subdirectories).
    Collection<File> oldFiles = FileUtils.listFiles(tempDir.getOutputDir().toFile(), filter, TrueFileFilter.INSTANCE);
		for (File file : oldFiles) {
			file.delete();
		}
	}


	/*Getter and Setters*/
	public int getMaxWait() {
		return maxWait;
	}

	public void setMaxWait(int maxWait) {
		this.maxWait = maxWait;
	}
}
