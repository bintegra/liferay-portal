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

package com.liferay.dynamic.data.mapping.internal.io;

import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldRenderer;
import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldType;
import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldTypeServicesTracker;
import com.liferay.dynamic.data.mapping.io.DDMFormFieldTypesSerializer;
import com.liferay.dynamic.data.mapping.io.DDMFormFieldTypesSerializerSerializeRequest;
import com.liferay.dynamic.data.mapping.io.DDMFormFieldTypesSerializerSerializeResponse;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Matchers;

import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author Marcellus Tavares
 */
public class DDMFormFieldTypesJSONSerializerTest extends BaseDDMTestCase {

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		setUpDDMFormFieldTypesJSONSerializer();
	}

	@Test
	public void testSerializationWithEmptyParameterList() throws Exception {
		List<DDMFormFieldType> ddmFormFieldTypes = Collections.emptyList();

		String actualJSON = serialize(ddmFormFieldTypes);

		Assert.assertEquals("[]", actualJSON);
	}

	@Test
	public void testSerializationWithNonemptyParameterList() throws Exception {
		List<DDMFormFieldType> ddmFormFieldTypes = new ArrayList<>();

		DDMFormFieldType ddmFormFieldType = getMockedDDMFormFieldType();

		ddmFormFieldTypes.add(ddmFormFieldType);

		String actualJSON = serialize(ddmFormFieldTypes);

		JSONAssert.assertEquals(createExpectedJSON(), actualJSON, false);
	}

	protected String createExpectedJSON() {
		JSONArray jsonArray = JSONUtil.put(
			JSONUtil.put(
				"icon", "my-icon"
			).put(
				"javaScriptClass", "myJavaScriptClass"
			).put(
				"javaScriptModule", "myJavaScriptModule"
			).put(
				"name", "Text"
			));

		return jsonArray.toString();
	}

	protected DDMFormFieldType getMockedDDMFormFieldType() {
		DDMFormFieldType ddmFormFieldType = mock(DDMFormFieldType.class);

		whenDDMFormFieldTypeGetName(ddmFormFieldType, "Text");

		return ddmFormFieldType;
	}

	protected DDMFormFieldTypeServicesTracker
		getMockedDDMFormFieldTypeServicesTracker() {

		DDMFormFieldTypeServicesTracker ddmFormFieldTypeServicesTracker = mock(
			DDMFormFieldTypeServicesTracker.class);

		DDMFormFieldRenderer ddmFormFieldRenderer = mock(
			DDMFormFieldRenderer.class);

		when(
			ddmFormFieldTypeServicesTracker.getDDMFormFieldRenderer(
				Matchers.anyString())
		).thenReturn(
			ddmFormFieldRenderer
		);

		when(
			ddmFormFieldTypeServicesTracker.getDDMFormFieldTypeProperties(
				Matchers.anyString())
		).thenReturn(
			HashMapBuilder.<String, Object>put(
				"ddm.form.field.type.icon", "my-icon"
			).put(
				"ddm.form.field.type.js.class.name", "myJavaScriptClass"
			).put(
				"ddm.form.field.type.js.module", "myJavaScriptModule"
			).build()
		);

		return ddmFormFieldTypeServicesTracker;
	}

	protected String serialize(List<DDMFormFieldType> ddmFormFieldTypes) {
		DDMFormFieldTypesSerializerSerializeRequest.Builder builder =
			DDMFormFieldTypesSerializerSerializeRequest.Builder.newBuilder(
				ddmFormFieldTypes);

		DDMFormFieldTypesSerializerSerializeResponse
			ddmFormFieldTypesSerializerSerializeResponse =
				_ddmFormFieldTypesSerializer.serialize(builder.build());

		return ddmFormFieldTypesSerializerSerializeResponse.getContent();
	}

	protected void setUpDDMFormFieldTypesJSONSerializer() throws Exception {
		Field field = ReflectionUtil.getDeclaredField(
			DDMFormFieldTypesJSONSerializer.class,
			"_ddmFormFieldTypeServicesTracker");

		field.set(
			_ddmFormFieldTypesSerializer,
			getMockedDDMFormFieldTypeServicesTracker());

		field = ReflectionUtil.getDeclaredField(
			DDMFormFieldTypesJSONSerializer.class, "_jsonFactory");

		field.set(_ddmFormFieldTypesSerializer, new JSONFactoryImpl());
	}

	protected void whenDDMFormFieldTypeGetName(
		DDMFormFieldType ddmFormFieldType, String returnName) {

		when(
			ddmFormFieldType.getName()
		).thenReturn(
			returnName
		);
	}

	private final DDMFormFieldTypesSerializer _ddmFormFieldTypesSerializer =
		new DDMFormFieldTypesJSONSerializer();

}