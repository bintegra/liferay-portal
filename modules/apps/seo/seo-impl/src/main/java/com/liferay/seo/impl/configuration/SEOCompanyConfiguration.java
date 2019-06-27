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

package com.liferay.seo.impl.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * @author Cristina González
 */
@ExtendedObjectClassDefinition(
	category = "pages", scope = ExtendedObjectClassDefinition.Scope.COMPANY
)
@Meta.OCD(
	description = "seo-configuration-description",
	id = "com.liferay.seo.impl.configuration.SEOCompanyConfiguration",
	localization = "content/Language", name = "seo-configuration-name"
)
public interface SEOCompanyConfiguration {

	/**
	 * Sets the type of configuration to be used with the localized URL
	 *
	 * @review
	 */
	@Meta.AD(
		deflt = SEOConfigurationConstants.DEFAULT,
		name = "seo-configuration-localized-url",
		optionLabels = {"default", "classic"},
		optionValues = {
			SEOConfigurationConstants.DEFAULT, SEOConfigurationConstants.CLASSIC
		},
		required = false
	)
	public String configuration();

}