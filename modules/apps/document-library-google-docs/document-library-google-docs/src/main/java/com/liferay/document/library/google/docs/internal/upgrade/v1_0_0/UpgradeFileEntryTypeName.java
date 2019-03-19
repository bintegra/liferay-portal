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

package com.liferay.document.library.google.docs.internal.upgrade.v1_0_0;

import com.liferay.document.library.google.docs.internal.util.GoogleDocsConstants;
import com.liferay.document.library.kernel.model.DLFileEntryType;
import com.liferay.document.library.kernel.service.DLFileEntryTypeLocalService;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.util.UpgradeProcessUtil;
import com.liferay.portal.kernel.util.LocaleUtil;

import java.sql.SQLException;

import java.util.Locale;

/**
 * @author Alejandro Tardín
 */
public class UpgradeFileEntryTypeName extends UpgradeProcess {

	public UpgradeFileEntryTypeName(
		DLFileEntryTypeLocalService dlFileEntryTypeLocalService) {

		_dlFileEntryTypeLocalService = dlFileEntryTypeLocalService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		try {
			ActionableDynamicQuery actionableDynamicQuery =
				_dlFileEntryTypeLocalService.getActionableDynamicQuery();

			actionableDynamicQuery.setAddCriteriaMethod(
				dynamicQuery -> dynamicQuery.add(
					RestrictionsFactoryUtil.eq(
						"fileEntryTypeKey",
						GoogleDocsConstants.DL_FILE_ENTRY_TYPE_KEY)));
			actionableDynamicQuery.setPerformActionMethod(
				(DLFileEntryType dlFileEntryType) ->
					_upgradeGoogleDocsDLFileEntryType(dlFileEntryType));

			actionableDynamicQuery.performActions();
		}
		catch (PortalException pe) {
			throw new UpgradeException(pe);
		}
	}

	private void _upgradeGoogleDocsDLFileEntryType(
			DLFileEntryType dlFileEntryType)
		throws UpgradeException {

		try {
			Locale locale = LocaleUtil.fromLanguageId(
				UpgradeProcessUtil.getDefaultLanguageId(
					dlFileEntryType.getCompanyId()));

			boolean hasDefaultDescription = "Google Docs".equals(
				dlFileEntryType.getDescription(locale));

			if (hasDefaultDescription) {
				dlFileEntryType.setDescription(
					GoogleDocsConstants.DL_FILE_ENTRY_TYPE_NAME, locale);
			}

			boolean hasDefaultName = "Google Docs".equals(
				dlFileEntryType.getName(locale));

			if (hasDefaultName) {
				dlFileEntryType.setName(
					GoogleDocsConstants.DL_FILE_ENTRY_TYPE_NAME, locale);
			}

			if (hasDefaultDescription || hasDefaultName) {
				_dlFileEntryTypeLocalService.updateDLFileEntryType(
					dlFileEntryType);
			}
		}
		catch (SQLException sqle) {
			throw new UpgradeException(sqle);
		}
	}

	private final DLFileEntryTypeLocalService _dlFileEntryTypeLocalService;

}