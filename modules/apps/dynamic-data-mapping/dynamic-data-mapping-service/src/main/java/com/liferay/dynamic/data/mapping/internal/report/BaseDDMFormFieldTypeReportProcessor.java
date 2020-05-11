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

package com.liferay.dynamic.data.mapping.internal.report;

import com.liferay.dynamic.data.mapping.report.DDMFormFieldTypeReportProcessor;
import com.liferay.dynamic.data.mapping.storage.DDMFormFieldValue;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;

/**
 * @author Marcos Martins
 */
public abstract class BaseDDMFormFieldTypeReportProcessor
	implements DDMFormFieldTypeReportProcessor {

	@Override
	public JSONObject process(
			DDMFormFieldValue ddmFormFieldValue,
			JSONObject formInstanceReportDataJSONObject,
			String formInstanceReportEvent)
		throws Exception {

		JSONObject cloneFormInstanceReportDataJSONObject =
			JSONFactoryUtil.createJSONObject(
				formInstanceReportDataJSONObject.toJSONString());

		if (!cloneFormInstanceReportDataJSONObject.has(
				ddmFormFieldValue.getName())) {

			cloneFormInstanceReportDataJSONObject.put(
				ddmFormFieldValue.getName(),
				JSONUtil.put(
					"type", ddmFormFieldValue.getType()
				).put(
					"values", JSONFactoryUtil.createJSONObject()
				));
		}

		return doProcess(
			ddmFormFieldValue, cloneFormInstanceReportDataJSONObject,
			formInstanceReportEvent);
	}

	protected abstract JSONObject doProcess(
			DDMFormFieldValue ddmFormFieldValue,
			JSONObject formInstanceReportDataJSONObject,
			String formInstanceReportEvent)
		throws Exception;

}