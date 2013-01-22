/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.osgi.bootstrap;

import aQute.libg.header.OSGiHeader;
import aQute.libg.version.Version;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.ServiceLoader;
import com.liferay.portal.kernel.util.ServiceLoaderCondition;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.util.PropsValues;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;

import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.context.ApplicationContext;

/**
 * @author Raymond Augé
 * @author Miguel Pastor
 */
public class ModuleFrameworkImpl
	implements ModuleFramework, ModuleFrameworkConstants {

	public Object addBundle(String location) throws PortalException {
		return addBundle(location, null);
	}

	public Object addBundle(String location, InputStream inputStream)
		throws PortalException {

		return addBundle(location, inputStream, true);
	}

	public Object addBundle(
			String location, InputStream inputStream, boolean checkPermission)
		throws PortalException {

		if (_framework == null) {
			return null;
		}

		if (checkPermission) {
			_checkPermission();
		}

		BundleContext bundleContext = _framework.getBundleContext();

		if (inputStream != null) {
			Bundle bundle = getBundle(bundleContext, inputStream);

			if (bundle != null) {
				return bundle;
			}
		}

		try {
			return bundleContext.installBundle(location, inputStream);
		}
		catch (BundleException be) {
			_log.error(be, be);

			throw new PortalException(be);
		}
	}

	public Bundle getBundle(
			BundleContext bundleContext, InputStream inputStream)
		throws PortalException {

		try {
			if (inputStream.markSupported()) {

				// 1 megabyte is more than enough for even the largest manifest
				// file

				inputStream.mark(1024 * 1000);
			}

			JarInputStream jarInputStream = new JarInputStream(inputStream);

			Manifest manifest = jarInputStream.getManifest();

			if (inputStream.markSupported()) {
				inputStream.reset();
			}

			Attributes attributes = manifest.getMainAttributes();

			String bundleSymbolicNameAttributeValue = attributes.getValue(
				Constants.BUNDLE_SYMBOLICNAME);

			Map<String, Map<String, String>> bundleSymbolicNameMap =
				OSGiHeader.parseHeader(bundleSymbolicNameAttributeValue);

			Set<String> bundleSymbolicNameSet = bundleSymbolicNameMap.keySet();

			Iterator<String> bundleSymbolicNameIterator =
				bundleSymbolicNameSet.iterator();

			String bundleSymbolicName = bundleSymbolicNameIterator.next();

			String bundleVersionAttributeValue = attributes.getValue(
				Constants.BUNDLE_VERSION);

			Version bundleVersion = Version.parseVersion(
				bundleVersionAttributeValue);

			for (Bundle bundle : bundleContext.getBundles()) {
				Version curBundleVersion = Version.parseVersion(
					bundle.getVersion().toString());

				if (bundleSymbolicName.equals(bundle.getSymbolicName()) &&
					bundleVersion.equals(curBundleVersion)) {

					return bundle;
				}
			}

			return null;
		}
		catch (IOException ioe) {
			throw new PortalException(ioe);
		}
	}

	public Bundle getBundle(long bundleId) {
		if (_framework == null) {
			return null;
		}

		BundleContext bundleContext = _framework.getBundleContext();

		return bundleContext.getBundle(bundleId);
	}

	public Framework getFramework() {
		return _framework;
	}

	public String getState(long bundleId) throws PortalException {
		_checkPermission();

		Bundle bundle = getBundle(bundleId);

		if (bundle == null) {
			throw new PortalException("No bundle with ID " + bundleId);
		}

		int state = bundle.getState();

		if (state == Bundle.ACTIVE) {
			return "active";
		}
		else if (state == Bundle.INSTALLED) {
			return "installed";
		}
		else if (state == Bundle.RESOLVED) {
			return "resolved";
		}
		else if (state == Bundle.STARTING) {
			return "starting";
		}
		else if (state == Bundle.STOPPING) {
			return "stopping";
		}
		else if (state == Bundle.UNINSTALLED) {
			return "uninstalled";
		}
		else {
			return StringPool.BLANK;
		}
	}

	public void registerContext(Object context) {
		if (context == null) {
			return;
		}

		if ((context instanceof ApplicationContext) &&
			PropsValues.MODULE_FRAMEWORK_REGISTER_LIFERAY_SERVICES) {

			ApplicationContext applicationContext = (ApplicationContext)context;

			_registerApplicationContext(applicationContext);
		}
		else if (context instanceof ServletContext) {
			ServletContext servletContext = (ServletContext)context;

			_registerServletContext(servletContext);
		}
	}

	public void setBundleStartLevel(long bundleId, int startLevel)
		throws PortalException {

		_checkPermission();

		Bundle bundle = getBundle(bundleId);

		if (bundle == null) {
			throw new PortalException("No bundle with ID " + bundleId);
		}

		BundleStartLevel bundleStartLevel = bundle.adapt(
			BundleStartLevel.class);

		bundleStartLevel.setStartLevel(startLevel);
	}

	public void startBundle(
			Bundle bundle, int options, boolean checkPermissions)
		throws PortalException {

		if (checkPermissions) {
			_checkPermission();
		}

		try {
			bundle.start(options);
		}
		catch (BundleException be) {
			_log.error(be, be);

			throw new PortalException(be);
		}
	}

	public void startBundle(long bundleId) throws PortalException {
		startBundle(bundleId, 0);
	}

	public void startBundle(long bundleId, int options) throws PortalException {
		Bundle bundle = getBundle(bundleId);

		if (bundle == null) {
			throw new PortalException("No bundle with ID " + bundleId);
		}

		startBundle(bundle, 0, true);
	}

	public void startFramework() throws Exception {
		ServiceLoaderCondition serviceLoaderCondition =
			new ModuleFrameworkServiceLoaderCondition();

		List<FrameworkFactory> frameworkFactories = ServiceLoader.load(
			FrameworkFactory.class, serviceLoaderCondition);

		if (frameworkFactories.isEmpty()) {
			return;
		}

		FrameworkFactory frameworkFactory = frameworkFactories.get(0);

		Map<String, String> properties = _buildProperties();

		_framework = frameworkFactory.newFramework(properties);

		_framework.init();

		_framework.start();

		_setupInitialBundles();
	}

	public void startRuntime() throws Exception {
		if (_framework == null) {
			return;
		}

		FrameworkStartLevel frameworkStartLevel = _framework.adapt(
			FrameworkStartLevel.class);

		frameworkStartLevel.setStartLevel(
			PropsValues.MODULE_FRAMEWORK_RUNTIME_START_LEVEL);
	}

	public void stopBundle(long bundleId) throws PortalException {
		stopBundle(bundleId, 0);
	}

	public void stopBundle(long bundleId, int options) throws PortalException {
		_checkPermission();

		Bundle bundle = getBundle(bundleId);

		if (bundle == null) {
			throw new PortalException("No bundle with ID " + bundleId);
		}

		try {
			bundle.stop(options);
		}
		catch (BundleException be) {
			_log.error(be, be);

			throw new PortalException(be);
		}
	}

	public void stopFramework() throws Exception {
		if (_framework == null) {
			return;
		}

		_framework.stop();
	}

	public void stopRuntime() throws Exception {
		if (_framework == null) {
			return;
		}

		FrameworkStartLevel frameworkStartLevel = _framework.adapt(
			FrameworkStartLevel.class);

		frameworkStartLevel.setStartLevel(
			PropsValues.MODULE_FRAMEWORK_BEGINNING_START_LEVEL);
	}

	public void uninstallBundle(long bundleId) throws PortalException {
		_checkPermission();

		Bundle bundle = getBundle(bundleId);

		if (bundle == null) {
			throw new PortalException("No bundle with ID " + bundleId);
		}

		try {
			bundle.uninstall();
		}
		catch (BundleException be) {
			_log.error(be, be);

			throw new PortalException(be);
		}
	}

	public void updateBundle(long bundleId) throws PortalException {
		updateBundle(bundleId, null);
	}

	public void updateBundle(long bundleId, InputStream inputStream)
		throws PortalException {

		_checkPermission();

		Bundle bundle = getBundle(bundleId);

		if (bundle == null) {
			throw new PortalException("No bundle with ID " + bundleId);
		}

		try {
			bundle.update(inputStream);
		}
		catch (BundleException be) {
			_log.error(be, be);

			throw new PortalException(be);
		}
	}

	private Map<String, String> _buildProperties() {
		Map<String, String> properties = new HashMap<String, String>();

		properties.put(
			Constants.BUNDLE_DESCRIPTION, ReleaseInfo.getReleaseInfo());
		properties.put(Constants.BUNDLE_NAME, ReleaseInfo.getName());
		properties.put(Constants.BUNDLE_VENDOR, ReleaseInfo.getVendor());
		properties.put(Constants.BUNDLE_VERSION, ReleaseInfo.getVersion());
		properties.put(
			Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
			String.valueOf(PropsValues.MODULE_FRAMEWORK_BEGINNING_START_LEVEL));
		properties.put(
			Constants.FRAMEWORK_BUNDLE_PARENT,
			Constants.FRAMEWORK_BUNDLE_PARENT_APP);
		properties.put(
			Constants.FRAMEWORK_STORAGE,
			PropsValues.MODULE_FRAMEWORK_STATE_DIR);

		// File install

		StringBundler sb = new StringBundler(3);

		sb.append(PropsValues.MODULE_FRAMEWORK_LIB_DIR);
		sb.append(StringPool.COMMA);
		sb.append(
			StringUtil.merge(PropsValues.MODULE_FRAMEWORK_AUTO_DEPLOY_DIRS));

		properties.put(FELIX_FILEINSTALL_DIR, sb.toString());
		properties.put(FELIX_FILEINSTALL_LOG_LEVEL, _getFileInstallLogLevel());
		properties.put(
			FELIX_FILEINSTALL_POLL,
			String.valueOf(PropsValues.MODULE_FRAMEWORK_AUTO_DEPLOY_INTERVAL));
		properties.put(
			FELIX_FILEINSTALL_TMPDIR, System.getProperty("java.io.tmpdir"));

		Properties extraProperties = PropsUtil.getProperties(
			"module.framework.properties.", true);

		for (Object key : extraProperties.keySet()) {
			String propertyKey = (String)key;

			String value = (String)extraProperties.get(propertyKey);

			// We need to support BLANK and null distinctly. This is due to some
			// different behaviors between OSGi implementations. So, if a value
			// is passed as property= it will be treated as BLANK. Otherwise,
			// property=null will be treated as an explicit 'null' reference.

			if (StringPool.NULL.equals(value)) {
				value = null;
			}

			properties.put(propertyKey, value);
		}

		return properties;
	}

	private void _checkPermission() throws PrincipalException {
		PermissionChecker permissionChecker =
			PermissionThreadLocal.getPermissionChecker();

		if ((permissionChecker == null) || !permissionChecker.isOmniadmin()) {
			throw new PrincipalException();
		}
	}

	private String _getFileInstallLogLevel() {

		// Felix file install uses a logging level scheme as follows:
		// NONE=0, ERROR=1, WARNING=2, INFO=3, DEBUG=4

		int fileInstallLogLevel = 0;

		if (_log.isDebugEnabled()) {
			fileInstallLogLevel = 4;
		}
		else if (_log.isErrorEnabled()) {
			fileInstallLogLevel = 1;
		}
		else if (_log.isInfoEnabled()) {
			fileInstallLogLevel = 3;
		}
		else if (_log.isWarnEnabled()) {
			fileInstallLogLevel = 2;
		}

		return String.valueOf(fileInstallLogLevel);
	}

	private Set<Class<?>> _getInterfaces(Object bean) {
		Set<Class<?>> interfaces = new HashSet<Class<?>>();

		Class<?> beanClass = bean.getClass();

		for (Class<?> interfaceClass : beanClass.getInterfaces()) {
			interfaces.add(interfaceClass);
		}

		while ((beanClass = beanClass.getSuperclass()) != null) {
			for (Class<?> interfaceClass : beanClass.getInterfaces()) {
				if (!interfaces.contains(interfaceClass)) {
					interfaces.add(interfaceClass);
				}
			}
		}

		return interfaces;
	}

	private boolean _hasLazyActivationPolicy(Bundle bundle) {
		Dictionary<String, String> headers = bundle.getHeaders();

		String fragmentHost = headers.get(Constants.FRAGMENT_HOST);

		if (fragmentHost != null) {
			return false;
		}

		String activationPolicy = headers.get(
			Constants.BUNDLE_ACTIVATIONPOLICY);

		if (activationPolicy == null) {
			return false;
		}

		Map<String, Map<String, String>> activationPolicyMap =
			OSGiHeader.parseHeader(activationPolicy);

		if (activationPolicyMap.containsKey(Constants.ACTIVATION_LAZY)) {
			return true;
		}

		return false;
	}

	private void _installInitialBundle(
		String location, List<Bundle> lazyActivationBundles,
		List<Bundle> startBundles, List<Bundle> refreshBundles) {

		boolean start = false;
		int startLevel = PropsValues.MODULE_FRAMEWORK_BEGINNING_START_LEVEL;

		int index = location.lastIndexOf(StringPool.AT);

		if (index != -1) {
			String[] parts = StringUtil.split(
				location.substring(index + 1), StringPool.COLON);

			for (String part : parts) {
				if (part.equals("start")) {
					start = true;
				}
				else {
					startLevel = GetterUtil.getInteger(part);
				}
			}

			location = location.substring(0, index);
		}

		InputStream inputStream = null;

		try {
			if (!location.startsWith("file:")) {
				location = "file:".concat(
					PropsValues.LIFERAY_LIB_PORTAL_DIR.concat(location));
			}

			URL initialBundleURL = new URL(location);

			try {
				inputStream = new BufferedInputStream(
					initialBundleURL.openStream());
			}
			catch (IOException ioe) {
				if (_log.isWarnEnabled()) {
					_log.warn(ioe.getMessage());
				}

				return;
			}

			Bundle bundle = (Bundle)addBundle(
				initialBundleURL.toString(), inputStream, false);

			if (bundle == null) {
				return;
			}

			if (_hasLazyActivationPolicy(bundle)) {
				lazyActivationBundles.add(bundle);

				return;
			}

			if (((bundle.getState() & Bundle.UNINSTALLED) == 0) &&
				(startLevel > 0)) {

				BundleStartLevel bundleStartLevel = bundle.adapt(
					BundleStartLevel.class);

				bundleStartLevel.setStartLevel(startLevel);
			}

			if (start) {
				startBundles.add(bundle);
			}

			if ((bundle.getState() & Bundle.INSTALLED) != 0) {
				refreshBundles.add(bundle);
			}
		}
		catch (Exception e) {
			_log.error(e, e);
		}
		finally {
			StreamUtil.cleanUp(inputStream);
		}
	}

	private void _registerApplicationContext(
		ApplicationContext applicationContext) {

		BundleContext bundleContext = _framework.getBundleContext();

		for (String beanName : applicationContext.getBeanDefinitionNames()) {
			Object bean = null;

			try {
				bean = applicationContext.getBean(beanName);
			}
			catch (BeanIsAbstractException biae) {
			}
			catch (Exception e) {
				_log.error(e, e);
			}

			if (bean != null) {
				_registerService(bundleContext, beanName, bean);
			}
		}
	}

	private void _registerService(
		BundleContext bundleContext, String beanName, Object bean) {

		Set<Class<?>> interfaces = _getInterfaces(bean);

		if (interfaces.isEmpty()) {
			return;
		}

		List<String> names = new ArrayList<String>(interfaces.size());

		for (Class<?> interfaceClass : interfaces) {
			names.add(interfaceClass.getName());
		}

		Hashtable<String, Object> properties = new Hashtable<String, Object>();

		properties.put(SERVICE_PROPERTY_KEY_BEAN_ID, beanName);
		properties.put(SERVICE_PROPERTY_KEY_ORIGINAL_BEAN, Boolean.TRUE);
		properties.put(
			SERVICE_PROPERTY_KEY_SERVICE_VENDOR, ReleaseInfo.getVendor());

		bundleContext.registerService(
			names.toArray(new String[names.size()]), bean, properties);
	}

	private void _registerServletContext(ServletContext servletContext) {
		BundleContext bundleContext = _framework.getBundleContext();

		Hashtable<String, Object> properties = new Hashtable<String, Object>();

		properties.put(
			SERVICE_PROPERTY_KEY_BEAN_ID, ServletContext.class.getName());
		properties.put(SERVICE_PROPERTY_KEY_ORIGINAL_BEAN, Boolean.TRUE);
		properties.put(
			SERVICE_PROPERTY_KEY_SERVICE_VENDOR, ReleaseInfo.getVendor());

		bundleContext.registerService(
			new String[] {ServletContext.class.getName()}, servletContext,
			properties);
	}

	private void _setupInitialBundles() throws Exception {
		FrameworkWiring frameworkWiring = getFramework().adapt(
			FrameworkWiring.class);

		List<Bundle> lazyActivationBundles = new ArrayList<Bundle>();
		List<Bundle> startBundles = new ArrayList<Bundle>();
		List<Bundle> refreshBundles = new ArrayList<Bundle>();

		for (String initialBundle :
				PropsValues.MODULE_FRAMEWORK_INITIAL_BUNDLES) {

			_installInitialBundle(
				initialBundle, lazyActivationBundles, startBundles,
				refreshBundles);
		}

		FrameworkListener frameworkListener = new StartupFrameworkListener(
			startBundles, lazyActivationBundles);

		frameworkWiring.refreshBundles(refreshBundles, frameworkListener);
	}

	private static Log _log = LogFactoryUtil.getLog(ModuleFrameworkUtil.class);

	private Framework _framework;

	private class ModuleFrameworkServiceLoaderCondition
		implements ServiceLoaderCondition {

		public boolean isLoad(URL url) {
			String path = url.getPath();

			return path.contains(PropsValues.MODULE_FRAMEWORK_CORE_DIR);
		}

	}

	private class StartupFrameworkListener implements FrameworkListener {

		public StartupFrameworkListener(
			List<Bundle> startBundles, List<Bundle> lazyActivationBundles) {

			_startBundles = startBundles;
			_lazyActivationBundles = lazyActivationBundles;
		}

		public void frameworkEvent(FrameworkEvent frameworkEvent) {
			if (frameworkEvent.getType() != FrameworkEvent.PACKAGES_REFRESHED) {
				return;
			}

			for (Bundle bundle : _startBundles) {
				try {
					startBundle(bundle, 0, false);
				}
				catch (Exception e) {
					_log.error(e, e);
				}
			}

			for (Bundle bundle : _lazyActivationBundles) {
				try {
					startBundle(bundle, Bundle.START_ACTIVATION_POLICY, false);
				}
				catch (Exception e) {
					_log.error(e, e);
				}
			}

			try {
				Bundle bundle = frameworkEvent.getBundle();

				BundleContext bundleContext = bundle.getBundleContext();

				bundleContext.removeFrameworkListener(this);
			}
			catch (Exception e) {
				_log.error(e, e);
			}
		}

		private List<Bundle> _lazyActivationBundles;
		private List<Bundle> _startBundles;

	}

}