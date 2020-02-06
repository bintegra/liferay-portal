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

import classNames from 'classnames';
import React from 'react';

import {LAYOUT_DATA_ITEM_DEFAULT_CONFIGURATIONS} from '../../config/constants/layoutDataItemDefaultConfigurations';
import {LAYOUT_DATA_ITEM_TYPES} from '../../config/constants/layoutDataItemTypes';

function Column({children, className, item, ...props}, ref) {
	const {
		config: {
			size = LAYOUT_DATA_ITEM_DEFAULT_CONFIGURATIONS[
				LAYOUT_DATA_ITEM_TYPES.column
			].size
		}
	} = item;

	return (
		<>
			<div
				className={classNames(className, 'col', {
					[`col-${size}`]: size
				})}
				ref={ref}
				{...props}
			>
				{children}
			</div>
		</>
	);
}

export default React.forwardRef(Column);
