/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ch.admin.bag.dp3t.stats;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import ch.admin.bag.dp3t.R;
import ch.admin.bag.dp3t.networking.models.HistoryDataPointModel;
import ch.admin.bag.dp3t.networking.models.StatsResponseModel;
import ch.admin.bag.dp3t.util.FormatUtil;
import ch.admin.bag.dp3t.util.UiUtils;
import ch.admin.bag.dp3t.util.UrlUtil;

public class StatsFragment extends Fragment {

	private static final int DIAGRAM_HISTORY_DAY_COUNT = 28;

	private StatsViewModel statsViewModel;

	Toolbar toolbar;
	private ScrollView scrollView;
	private ImageView headerView;

	private TextView totalActiveusers;
	private TextView totalActiveusersText;

	private Group covidcodesGroup;
	private TextView totalCovidcodesEntered;
	private TextView totalCovidcodesEnteredLastTwoDays;

	private ViewGroup casesSevenDayAverageContainer;
	private TextView casesSevenDayAverage;
	private ViewGroup casesPreviousWeekChangeContainer;
	private TextView casesPreviousWeekChange;

	private View diagramBox;
	private DiagramView diagramView;
	private HorizontalScrollView diagramScrollView;
	private View scrollViewWidener;
	private DiagramYAxisView diagramYAxisView;

	private View errorView;
	private TextView errorRetryButton;
	private View progressView;

	private View moreStatsButton;

	private Button shareAppButton;

	public static StatsFragment newInstance() {
		return new StatsFragment();
	}

	public StatsFragment() {
		super(R.layout.fragment_stats);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		statsViewModel = new ViewModelProvider(requireActivity()).get(StatsViewModel.class);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		toolbar = view.findViewById(R.id.main_toolbar);
		scrollView = view.findViewById(R.id.stats_scroll_view);
		headerView = view.findViewById(R.id.header_view);

		totalActiveusers = view.findViewById(R.id.stats_total_active_users);
		totalActiveusersText = view.findViewById(R.id.stats_total_active_users_text);

		covidcodesGroup = view.findViewById(R.id.stats_covidcodes_group);
		totalCovidcodesEntered = view.findViewById(R.id.stats_covidcodes_total_value);
		totalCovidcodesEnteredLastTwoDays = view.findViewById(R.id.stats_covidcodes_two_days_value);

		casesSevenDayAverageContainer = view.findViewById(R.id.stats_cases_seven_day_average);
		casesSevenDayAverage = view.findViewById(R.id.stats_cases_seven_day_average_value);
		casesPreviousWeekChangeContainer = view.findViewById(R.id.stats_cases_previous_week_change);
		casesPreviousWeekChange = view.findViewById(R.id.stats_cases_previous_week_change_value);

		diagramBox = view.findViewById(R.id.diagram_box);
		diagramView = view.findViewById(R.id.diagram_view);
		diagramScrollView = view.findViewById(R.id.diagram_scroll_view);
		scrollViewWidener = view.findViewById(R.id.scroll_view_widener);
		diagramYAxisView = view.findViewById(R.id.diagram_y_axis_view);

		errorView = view.findViewById(R.id.error_view);
		errorRetryButton = view.findViewById(R.id.button_retry);
		progressView = view.findViewById(R.id.progress_view);

		moreStatsButton = view.findViewById(R.id.stats_more);

		shareAppButton = view.findViewById(R.id.share_app_button);

		setupScrollBehavior();
		setupDiagramScrollBehavior();
		setupMoreStatsButton();
		setupShareAppButton();

		statsViewModel.getStatsLiveData().observe(getViewLifecycleOwner(), this::displayStats);
	}

	private void setupScrollBehavior() {
		int scrollRangePx = getResources().getDimensionPixelSize(R.dimen.top_item_padding);
		int translationRangePx = -getResources().getDimensionPixelSize(R.dimen.spacing_huge);

		scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
			float progress = UiUtils.computeScrollAnimProgress(scrollY, scrollRangePx);
			headerView.setAlpha(1 - progress);
			headerView.setTranslationY(progress * translationRangePx);
		});
		scrollView.post(() -> {
			float progress = UiUtils.computeScrollAnimProgress(scrollView.getScrollY(), scrollRangePx);
			headerView.setAlpha(1 - progress);
			headerView.setTranslationY(progress * translationRangePx);
		});
	}

	private void setupDiagramScrollBehavior() {
		diagramScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
			diagramView.setScrollX(scrollX);
		});
	}

	private void setupMoreStatsButton() {
		moreStatsButton.setOnClickListener(v -> {
			String url = v.getContext().getResources().getString(R.string.stats_more_statistics_url);
			UrlUtil.openUrl(v.getContext(), url);
		});
	}

	private void setupShareAppButton() {
		shareAppButton.setOnClickListener(v -> {
			String message = v.getContext().getResources().getString(R.string.share_app_message) + "\n" +
					v.getContext().getResources().getString(R.string.share_app_url);

			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_TEXT, message);
			intent.setType("text/pain");

			Intent shareIntent = Intent.createChooser(intent, null);
			startActivity(shareIntent);
		});
	}

	private void displayStats(StatsOutcome outcome) {
		switch (outcome.getOutcome()) {
			case LOADING:
				totalActiveusers.setVisibility(View.INVISIBLE);
				totalActiveusersText.setVisibility(View.INVISIBLE);

				covidcodesGroup.setVisibility(View.INVISIBLE);

				casesSevenDayAverageContainer.setVisibility(View.INVISIBLE);
				casesPreviousWeekChangeContainer.setVisibility(View.INVISIBLE);

				diagramBox.setVisibility(View.GONE);
				diagramYAxisView.setVisibility(View.GONE);
				errorView.setVisibility(View.GONE);
				errorRetryButton.setVisibility(View.GONE);
				progressView.setVisibility(View.VISIBLE);
				break;
			case ERROR:
				totalActiveusers.setVisibility(View.INVISIBLE);
				totalActiveusersText.setVisibility(View.INVISIBLE);

				covidcodesGroup.setVisibility(View.INVISIBLE);

				casesSevenDayAverageContainer.setVisibility(View.INVISIBLE);
				casesPreviousWeekChangeContainer.setVisibility(View.INVISIBLE);

				diagramBox.setVisibility(View.GONE);
				diagramYAxisView.setVisibility(View.GONE);
				errorView.setVisibility(View.VISIBLE);
				errorRetryButton.setVisibility(View.VISIBLE);
				progressView.setVisibility(View.GONE);

				errorRetryButton.setPaintFlags(errorRetryButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
				errorRetryButton.setOnClickListener(v -> statsViewModel.loadStats());
				break;
			case RESULT:
				StatsResponseModel stats = outcome.getStatsResponseModel();

				totalActiveusers.setVisibility(View.VISIBLE);
				totalActiveusersText.setVisibility(View.VISIBLE);

				covidcodesGroup.setVisibility(View.VISIBLE);

				casesSevenDayAverageContainer.setVisibility(View.VISIBLE);
				casesPreviousWeekChangeContainer.setVisibility(View.VISIBLE);

				diagramBox.setVisibility(View.VISIBLE);
				diagramYAxisView.setVisibility(View.VISIBLE);
				// lastUpdated.visibility is set below
				errorView.setVisibility(View.GONE);
				errorRetryButton.setVisibility(View.GONE);
				progressView.setVisibility(View.GONE);

				String text = totalActiveusers.getContext().getResources().getString(R.string.stats_counter);
				text = text.replace("{COUNT}", FormatUtil.formatNumberInMillions(stats.getTotalActiveUsers()));
				totalActiveusers.setText(text);

				totalCovidcodesEntered.setText(FormatUtil.formatNumberInThousands(stats.getTotalCovidcodesEntered()));
				totalCovidcodesEnteredLastTwoDays.setText(FormatUtil.formatPercentage(stats.getTotalCovidcodesEntered0to2d(), 0));

				casesSevenDayAverage.setText(FormatUtil.formatNumberInThousands(stats.getNewInfectionsSevenDayAvg()));
				casesPreviousWeekChange.setText(FormatUtil.formatPercentage(stats.getNewInfectionsSevenDayAvgRelPrevWeek(), 0));

				List<HistoryDataPointModel> fullHistory = stats.getHistory();
				List<HistoryDataPointModel> diagramHistory =
						fullHistory.subList(fullHistory.size() - DIAGRAM_HISTORY_DAY_COUNT, fullHistory.size());
				diagramView.setHistory(diagramHistory);
				diagramYAxisView.setMaxYValue(DiagramView.findMaxYValue(diagramHistory));

				int requiredWidth = diagramView.getTotalTheoreticWidth();
				// Setting the width via LayoutParams does NOT work for the direct child of a ScrollView!
				scrollViewWidener.setMinimumWidth(requiredWidth);

				diagramScrollView.post(() -> {
					diagramScrollView.scrollTo(requiredWidth, 0);
					//make sure scroll position of diagramView gets also updated, also if scrollposition of diagramScrollView is
					// already at requiredWidth
					diagramView.setScrollX(diagramScrollView.getScrollX());
				});
		}
	}

}
