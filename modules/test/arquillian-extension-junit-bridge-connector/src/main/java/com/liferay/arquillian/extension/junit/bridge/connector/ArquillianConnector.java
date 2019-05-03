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

package com.liferay.arquillian.extension.junit.bridge.connector;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * @author Matthew Tambara
 */
@Component(
	configurationPid = "com.liferay.arquillian.extension.junit.bridge.connector.ArquillianConnectorConfiguration",
	immediate = true, service = {}
)
public class ArquillianConnector {

	@Activate
	public void activate(
		BundleContext bundleContext, Map<String, String> properties) {

		ArquillianConnectorConfiguration arquillianConnectorConfiguration =
			ConfigurableUtil.createConfigurable(
				ArquillianConnectorConfiguration.class, properties);

		int port = arquillianConnectorConfiguration.port();

		_thread = new Thread(
			new ArquillianConnectorRunnable(bundleContext, port),
			"Arquillian-Connector-Thread");

		_thread.setDaemon(true);

		_thread.start();
	}

	@Deactivate
	public void deacticate() {
		_thread.interrupt();
	}

	private Thread _thread;

}