/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pivot_stuff.extensions.preferences;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.MissingResourceException;

import javax.jnlp.BasicService;
import javax.jnlp.FileContents;
import javax.jnlp.PersistenceService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.Dictionary;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.Map;
import org.apache.pivot.serialization.SerializationException;

/**
 * This class reads and writes a Tree of Preferences in a JSON format, where
 * each Node is a Dictionary<String, Object>. <br/>
 * This implementation reads and writes data from the (local) FileSystem, but
 * using the Web Start PersistenceService, so must be used in an
 * Applet/Application executed from a JNLP file, otherwise the various Services
 * used will be null. <br/>
 * Starting from Java 6U10 it's possible also from Applets, but using a jnlp
 * file. <br/>
 * Note that this class needs the Java Web Start jar (javaws.jar, usually under
 * jre/lib) to compile and run. <br/>
 * <p>
 * Note that all restrictions of PersistenceService are inherited here.
 * </p>
 * 
 * @see java.util.prefs.Preferences
 * @see pivot.util.Resources
 */
public class PreferencesWebStart extends Preferences implements
		Dictionary<String, Object> {

	protected static final String CLASS_NAME = PreferencesWebStart.class
			.getName();

	URL codebase = null;
	BasicService basicService = null;
	PersistenceService persistenceService = null;
	FileContents dataStored = null;

	/** Default Constructor */
	public PreferencesWebStart() throws IOException, SerializationException {
		super();

		setInitialized(this.init());
	}

	/**
	 * Full constructor for a Preferences instance, setting non-variant
	 * attributes.
	 * 
	 * @param applicationName
	 *            the base name of this preferences set, for example a fully
	 *            qualified class name, or the name of the application for
	 *            grouping more preferences contexts, or if null the current
	 *            class name will be used
	 * @throws IOException
	 *             if there is a problem when reading the resource
	 * @throws SerializationException
	 *             if there is a problem deserializing the resource from its
	 *             JSON format
	 * @throws NullPointerException
	 *             if baseName is null
	 * @throws MissingResourceException
	 *             if no preferences for the specified base name can be found
	 */
	public PreferencesWebStart(String applicationName) throws IOException,
			SerializationException {
		super(applicationName);

		setInitialized(this.init());
	}

	/**
	 * Construction-only initialization.
	 * 
	 * @return true if successful initialization, otherwise false
	 * @see Preferences#init()
	 */
	private boolean init() {
		boolean inited = false;

		try {
			basicService = (BasicService) ServiceManager
					.lookup("javax.jnlp.BasicService");
			persistenceService = (PersistenceService) ServiceManager
					.lookup("javax.jnlp.PersistenceService");
			codebase = basicService.getCodeBase();

			inited = createPreferencesPath();

			inited = true;
		} catch (UnavailableServiceException use) {
			use.printStackTrace();
		}

		return inited;
	}

	/**
	 * Set the root path for all Preferences (in an implementation-dependent
	 * way).
	 * 
	 * @see Preferences
	 */
	@Override
	protected void setRootPath() {
		if (codebase != null)
			this.rootPath = codebase.toString();
		else
			this.rootPath = null;
	}

	/**
	 * Set the base path for all Preferences
	 * 
	 * @see Preferences
	 */
	@Override
	protected void setBasePath(String basePath) {
		this.basePath = PIVOT_PREFS_DIR.replaceAll("\\.", "_")
				+ getPathSeparator();
	}

	/**
	 * Returns the character to use as separator at the end of the Path part,
	 * and before the name.
	 */
	@Override
	protected String getPathSeparator() {
		return "_";
	}

	/**
	 * Set the base name for all Preferences of this application
	 * 
	 * @see Preferences
	 */
	@Override
	protected void setBaseName(String baseName) {
		this.baseName = getApplicationName(); // flat name version
	}

	/**
	 * Creates all the directory structure for the storage of all Preferences
	 * for the current application (using applicationName, given in the
	 * constructor).
	 * <p>
	 * Note that in this implementation it's not possible to create/delete
	 * directories, so this returns always false.
	 * </p>
	 * 
	 * @return true if successful
	 */
	@Override
	protected boolean createPreferencesPath() {
		boolean success = false;
		// String prefsPath = getPreferencesPath();

		setRootPath(); // here i must call this method now, after the init()

		return success;
	}

	/**
	 * Delete all the directory structure for the storage of all Preferences for
	 * the current application (using applicationName, given in the
	 * constructor).
	 * <p>
	 * Attention, this is usually not needed by most applications, if not for
	 * cleanup operations.
	 * </p>
	 * <p>
	 * Note that in this implementation it's not possible to create/delete
	 * directories, so this returns always false.
	 * </p>
	 * 
	 * @return true if successful
	 */
	@Override
	protected boolean deletePreferencesPath() {
		boolean success = false;
		// String prefsPath = getPreferencesPath();

		return success;
	}

	@Override
	protected void writePreferences() throws IOException,
			SerializationException {
		String fileName = getContextResourceName();
		URL fileUrl = null;
		OutputStream out = null;

		if (getPrefsMap() == null) {
			// verify if in this case it's better to delete the related file ...
			throw new SerializationException("preferences map is null.");
		}

		try {
			fileUrl = new URL(fileName);

			try {
				// delete before create, but if not exist throws exception
				// (otherwise if existing, there will be impossible to write to
				// it)
				persistenceService.delete(fileUrl);
			} catch (FileNotFoundException fnfe) {
				// fnfe.printStackTrace();
				; // silently ignore this, ok
			}

			persistenceService.create(fileUrl, 4096); // TODO: remove hard-coded
														// value ...
			dataStored = persistenceService.get(fileUrl);
			out = new ObjectOutputStream(dataStored.getOutputStream(false)); // write
																				// without
																				// append
			if (out == null) {
				throw new SerializationException(
						"outputStream is null, unable to write to  \""
								+ fileName + "\".");
			}

			getSerializer().writeObject(getPrefsMap(), out);

		} catch (Exception e) {
			e.printStackTrace();
			throw new SerializationException("unable to write: "
					+ e.getMessage(), e);
		} finally {
			if (out != null)
				out.close();
		}

	}

	@Override
	@SuppressWarnings("unchecked")
	protected Map<String, Object> readPreferences() throws IOException,
			SerializationException {
		String fileName = getContextResourceName();
		URL fileUrl = null;
		InputStream in = null;

		Map<String, Object> resourceMap = null;

		try {
			fileUrl = new URL(fileName);

			dataStored = persistenceService.get(fileUrl);
			in = new ObjectInputStream(dataStored.getInputStream());
			if (in == null) {
				throw new SerializationException(
						"inputStream is null, unable to read from \""
								+ fileName + "\".");
			}

			resourceMap = (Map<String, Object>) getSerializer().readObject(in);

		} catch (Exception e) {
			e.printStackTrace();
			throw new SerializationException("unable to read: "
					+ e.getMessage(), e);
		} finally {
			if (in != null)
				in.close();
		}

		return resourceMap;
	}

	@Override
	public boolean existPreferences() throws IOException,
			SerializationException {
		boolean success = false;
		String fileName = getContextResourceName();
		URL fileUrl = null;

		try {
			fileUrl = new URL(fileName);

			dataStored = persistenceService.get(fileUrl);
			if (dataStored != null)
				success = true;

		} catch (Exception e) {
			e.printStackTrace();
			throw new SerializationException("unable to check for existence: "
					+ e.getMessage(), e);
		}

		return success;
	}

	@Override
	public boolean deletePreferences() throws IOException,
			SerializationException {
		boolean success = false;
		String fileName = getContextResourceName();
		URL fileUrl = null;

		try {
			fileUrl = new URL(fileName);

			persistenceService.delete(fileUrl);

		} catch (Exception e) {
			e.printStackTrace();
			throw new SerializationException("unable to delete: "
					+ e.getMessage(), e);
		}

		return success;
	}

	@Override
	public String dumpPreferences() throws IOException, SerializationException {
		String dump = null;

		Map<String, Object> resourceMap = null;
		ByteArrayOutputStream outputStream = null;

		try {
			resourceMap = readPreferences();

			try {
				outputStream = new ByteArrayOutputStream();
				getSerializer().writeObject(resourceMap, outputStream);
			} finally {
				outputStream.close();

				dump = new String(outputStream.toByteArray());
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new SerializationException("unable to list: "
					+ e.getMessage(), e);
		} finally {
		}

		return dump;
	}

	@Override
	public List<String> listPreferencesContexts() throws IOException,
			SerializationException {
		List<String> list = null;
		String prefsPath = getPreferencesPath();
		URL fileUrl = null;
		String[] files = null;

		try {
			fileUrl = new URL(prefsPath);

			files = persistenceService.getNames(fileUrl);

			if (files == null)
				return list;
			// else ...
			list = new ArrayList<String>();

			for (int i = 0; i < files.length; i++) {
				String name = files[i];

				// remove file extension from the file found
				int lastDotIndex = name.lastIndexOf('.');

				list.add(name.substring(0, lastDotIndex));
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new SerializationException("unable to list: "
					+ e.getMessage(), e);
		}

		return list;
	}

	@Override
	public String toString() {
		String parent = super.toString();

		return parent + " + " + CLASS_NAME + " : { " + "codebase: \""
				+ codebase + "\", " + "persistenceService: "
				+ persistenceService + " " + " }";
	}

}
