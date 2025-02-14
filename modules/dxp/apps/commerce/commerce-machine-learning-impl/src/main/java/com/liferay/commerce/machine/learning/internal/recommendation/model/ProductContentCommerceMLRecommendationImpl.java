/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */

package com.liferay.commerce.machine.learning.internal.recommendation.model;

import com.liferay.commerce.machine.learning.recommendation.model.ProductContentCommerceMLRecommendation;

/**
 * @author Riccardo Ferrari
 */
public class ProductContentCommerceMLRecommendationImpl
	extends BaseCommerceMLRecommendationImpl
	implements ProductContentCommerceMLRecommendation {

	@Override
	public int getRank() {
		return _rank;
	}

	@Override
	public void setRank(int rank) {
		_rank = rank;
	}

	private int _rank;

}