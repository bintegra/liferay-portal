/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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

package com.liferay.split.packages.test;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.util.HashUtil;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.module.framework.ModuleFrameworkUtilAdapter;

import java.io.IOException;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * @author Tom Wang
 */
@RunWith(Arquillian.class)
public class SplitPackagesTest {

	@Test
	public void testSplitPackage() throws IOException {
		Bundle frameworkBundle =
			(Bundle)ModuleFrameworkUtilAdapter.getFramework();

		BundleContext frameworkBundleContext =
			frameworkBundle.getBundleContext();

		Bundle[] frameworkBundles = frameworkBundleContext.getBundles();

		Map<Bundle, Map<String, ExportPackage>> bundlesMap = new HashMap<>();

		Map<String, SplitPackages> allowedSplitPackagesMap =
			_getAllowedSplitPackagesMap();

		for (Bundle bundle : frameworkBundles) {
			Map<String, ExportPackage> bundleExportPackages =
				_getBundleExportPackages(bundle);

			if (bundleExportPackages == null) {
				continue;
			}

			for (Map.Entry<Bundle, Map<String, ExportPackage>> entry :
					bundlesMap.entrySet()) {

				Map<String, ExportPackage> mapBundlePackages = new HashMap<>(
					entry.getValue());

				Set<String> keySet = mapBundlePackages.keySet();

				keySet.retainAll(bundleExportPackages.keySet());

				if (!mapBundlePackages.isEmpty()) {
					_processDuplicatedPackages(
						entry.getKey(), mapBundlePackages.values(),
						bundleExportPackages, allowedSplitPackagesMap,
						bundle.getSymbolicName());
				}
			}

			bundlesMap.put(bundle, bundleExportPackages);
		}
	}

	private boolean _checkAllowedSplitPackages(
		SplitPackages allowedSplitPackages, ExportPackage exportPackage,
		String currentSymbolicName) {

		if (exportPackage.equals(allowedSplitPackages.getExportPackage()) &&
			allowedSplitPackages.hasBundle(currentSymbolicName)) {

			return true;
		}

		return false;
	}

	private Map<String, SplitPackages> _getAllowedSplitPackagesMap()
		throws IOException {

		Map<String, SplitPackages> allowedSplitPackagesMap = new HashMap<>();

		for (String splitPackagesLine :
				StringUtil.splitLines(
					StringUtil.read(
						SplitPackagesTest.class.getResourceAsStream(
							"AllowedSplitPackages.txt")))) {

			String[] splitPackagesParts = StringUtil.split(
				splitPackagesLine, ';');

			allowedSplitPackagesMap.put(
				splitPackagesParts[0],
				new SplitPackages(
					new ExportPackage(
						splitPackagesParts[0], splitPackagesParts[1]),
					SetUtil.fromArray(
						StringUtil.split(splitPackagesParts[2]))));
		}

		return allowedSplitPackagesMap;
	}

	private Map<String, ExportPackage> _getBundleExportPackages(Bundle bundle) {
		Dictionary<String, String> headers = bundle.getHeaders();

		String exportPackage = headers.get(Constants.EXPORT_PACKAGE);

		if (exportPackage == null) {
			return null;
		}

		Parameters parameters = OSGiHeader.parseHeader(exportPackage);

		Map<String, ? extends Map<String, String>> exportPackages =
			parameters.asMapMap();

		Map<String, ExportPackage> bundleExportPackages = new HashMap<>();

		for (Map.Entry<String, ? extends Map<String, String>> entry :
				exportPackages.entrySet()) {

			String packageName = entry.getKey();

			Map<String, String> attributes = entry.getValue();

			String version = attributes.get(Constants.VERSION_ATTRIBUTE);

			if (version == null) {
				version = "0.0";
			}

			bundleExportPackages.put(
				packageName, new ExportPackage(packageName, version));
		}

		return bundleExportPackages;
	}

	private void _processDuplicatedPackages(
		Bundle mapBundle, Collection<ExportPackage> duplicatedExportPackages,
		Map<String, ExportPackage> currentBundleExportPackages,
		Map<String, SplitPackages> allowedSplitPackagesMap,
		String currentSymbolicName) {

		String symbolicName = mapBundle.getSymbolicName();

		for (ExportPackage duplicatedExportPackage : duplicatedExportPackages) {
			String duplicatedPackageName = duplicatedExportPackage._name;

			ExportPackage exportPackage = currentBundleExportPackages.get(
				duplicatedPackageName);

			if (Objects.equals(
					exportPackage._version, duplicatedExportPackage._version)) {

				Assert.assertTrue(
					"Detected split packages in " + currentSymbolicName +
						" and " + symbolicName + ": " + duplicatedPackageName,
					allowedSplitPackagesMap.containsKey(duplicatedPackageName));

				Assert.assertTrue(
					"Detected split packages in " + currentSymbolicName +
						" and " + symbolicName + ": " +
							duplicatedExportPackages,
					_checkAllowedSplitPackages(
						allowedSplitPackagesMap.get(duplicatedPackageName),
						exportPackage, currentSymbolicName));
			}
		}
	}

	private class ExportPackage {

		@Override
		public boolean equals(Object obj) {
			ExportPackage exportPackage = (ExportPackage)obj;

			if (Objects.equals(_name, exportPackage._name) &&
				Objects.equals(_version, exportPackage._version)) {

				return true;
			}

			return false;
		}

		@Override
		public int hashCode() {
			int hashCode = HashUtil.hash(0, _name);

			return HashUtil.hash(hashCode, _version);
		}

		private ExportPackage(String name, String version) {
			_name = name;
			_version = version;
		}

		private final String _name;
		private final String _version;

	}

	private class SplitPackages {

		public ExportPackage getExportPackage() {
			return _exportPackage;
		}

		public boolean hasBundle(String symbolicName) {
			return _symbolicNames.contains(symbolicName);
		}

		private SplitPackages(
			ExportPackage exportPackage, Set<String> symbolicNames) {

			_exportPackage = exportPackage;
			_symbolicNames = symbolicNames;
		}

		private final ExportPackage _exportPackage;
		private final Set<String> _symbolicNames;

	}

}