package com.sainbres.shu.weddingflow.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.sainbres.shu.weddingflow.BudgetTabFragments.BudgetBreakdownFragment;
import com.sainbres.shu.weddingflow.BudgetTabFragments.BudgetOverviewFragment;
import com.sainbres.shu.weddingflow.Models.InitialBudget;
import com.sainbres.shu.weddingflow.Models.InitialBudget_Table;
import com.sainbres.shu.weddingflow.Models.WeddingEvent;
import com.sainbres.shu.weddingflow.Models.WeddingEvent_Table;
import com.sainbres.shu.weddingflow.R;
import com.sainbres.shu.weddingflow.SetupInitialBudgetActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class BudgetFragment extends Fragment {

    private SharedPreferences SharedPrefs;
    private SharedPreferences.Editor Editor;
    int eventId;

    private View view;

    public BudgetFragment() {
        // Required empty public constructor
    }

    public static BudgetFragment newInstance(String param1, String param2) {
        BudgetFragment fragment = new BudgetFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_budget, container, false);

        InitialBudget budget;

        SharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Editor = SharedPrefs.edit();

        eventId = SharedPrefs.getInt(getString(R.string.SP_EventId), -1);
        if (eventId != -1){
            budget = SQLite.select()
                    .from(InitialBudget.class)
                    .where(InitialBudget_Table.EventId.eq(eventId))
                    .querySingle();

            if (budget != null) {
                setupGraph(budget);
            }
            else{
                Intent intent = new Intent(getActivity(), SetupInitialBudgetActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        }

        TabLayout tabLayout = view.findViewById(R.id.tablayout);
        tabLayout.addTab(tabLayout.newTab().setText("Overview"));
        tabLayout.addTab(tabLayout.newTab().setText("Breakdown"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = view.findViewById(R.id.pager);
        final com.sainbres.shu.weddingflow.PagerAdapter adapter = new com.sainbres.shu.weddingflow.PagerAdapter(getChildFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.setOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab){
                viewPager.setCurrentItem(tab.getPosition());
            }


            @Override
            public void onTabUnselected(TabLayout.Tab tab){

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab){

            }

        });
        tabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.colorWhite));
        tabLayout.setTabTextColors(getResources().getColor(R.color.colorWhiteTrans), getResources().getColor(R.color.colorWhite));

        return view;
    }

    private void setupGraph(InitialBudget budget){
        GraphView graph = (GraphView) view.findViewById(R.id.graph);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        try {

            WeddingEvent event = SQLite.select()
                    .from(WeddingEvent.class)
                    .where(WeddingEvent_Table.EventId.eq(eventId))
                    .querySingle();

            String savingsStartDateStr = budget.getSavingsStartDate();
            Date savingsStartDate = sdf.parse(savingsStartDateStr);

            String savingsEndDateStr = event.getWeddingDate();
            Date savingsEndDate = sdf.parse(savingsEndDateStr);
            double ongoingSavings = budget.getSavingsStart();
            //Date currentDate = new Date();
            DataPoint initialSavings = new DataPoint(savingsStartDate, ongoingSavings);


            Calendar cal = Calendar.getInstance();
            Date d1 = cal.getTime();
            cal.add(Calendar.DATE, 1);
            Date d2 = cal.getTime();
            cal.add(Calendar.DATE, 2);
            Date d3 = cal.getTime();



            Date periodicPaymentDate = savingsStartDate;
            String periodicity = budget.getSavingsPeriodicity();
            ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
            dataPoints.add(initialSavings);
            do {
                if (periodicity.equals("Monthly")){
                    cal.setTime(periodicPaymentDate);
                    cal.add(Calendar.MONTH, 1);
                    periodicPaymentDate = cal.getTime();
                }
                ongoingSavings = ongoingSavings + budget.getSavingsPeriodic();
                dataPoints.add(new DataPoint(periodicPaymentDate, ongoingSavings));
            } while (periodicPaymentDate.before(savingsEndDate));
            LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints.toArray(new DataPoint[dataPoints.size()]));
            graph.addSeries(series);
            //graph.getGridLabelRenderer().setHorizontalAxisTitle("Months");
            //graph.getGridLabelRenderer().setVerticalAxisTitle("£");

            graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity()));
            graph.getGridLabelRenderer().setNumHorizontalLabels(3); // only 4 because of the space

            cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);
            Date min = cal.getTime();
            cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 1);
            Date max = cal.getTime();

            graph.getViewport().setMinX(min.getTime());
            graph.getViewport().setMaxX(max.getTime());
            graph.getViewport().setXAxisBoundsManual(true);


            graph.getViewport().setMinY(0);
            graph.getViewport().setMaxY(4000);
            graph.getViewport().setYAxisBoundsManual(true);

            graph.getViewport().setScrollable(true); // enables horizontal scrolling

            graph.getGridLabelRenderer().setHumanRounding(false);
            //graph.getGridLabelRenderer().setPadding(60);
            graph.getGridLabelRenderer().setHorizontalLabelsAngle(45);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}