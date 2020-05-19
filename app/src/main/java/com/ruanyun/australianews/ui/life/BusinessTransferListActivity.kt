package com.ruanyun.australianews.ui.life

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.ruanyun.australianews.R
import com.ruanyun.australianews.base.BaseActivity
import com.ruanyun.australianews.base.PageInfoBase
import com.ruanyun.australianews.base.ResultBase
import com.ruanyun.australianews.base.refreshview.data.IDataDelegate
import com.ruanyun.australianews.base.refreshview.data.IDataSource
import com.ruanyun.australianews.base.refreshview.impl.PageDataSource
import com.ruanyun.australianews.base.refreshview.impl.PtrRefreshViewHolder
import com.ruanyun.australianews.base.refreshview.impl.RvHeaderFootViewAdapter
import com.ruanyun.australianews.data.ApiService
import com.ruanyun.australianews.ext.click
import com.ruanyun.australianews.model.Event
import com.ruanyun.australianews.model.LifeBusinessTransferInfo
import com.ruanyun.australianews.model.ParentCodeInfo
import com.ruanyun.australianews.model.params.LifeBusinessTransferParams
import com.ruanyun.australianews.ui.adapter.LifeReleaseCommonAdapter
import com.ruanyun.australianews.ui.life.release.ReleaseBusinessTransferActivity
import com.ruanyun.australianews.ui.wealth.adapter.WealthCommonAdapter
import com.ruanyun.australianews.util.*
import com.ruanyun.australianews.widget.filterpopwindow.FilterInfoUiModel
import com.ruanyun.australianews.widget.filterpopwindow.FilterListPopupWindow
import com.ruanyun.australianews.widget.filterpopwindow.OnFilterClickListener
import kotlinx.android.synthetic.main.activity_life_business_transfer_list.*
import kotlinx.android.synthetic.main.layout_refresh_rv_common.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import rx.Observable
import java.util.ArrayList
import javax.inject.Inject

/**
 * @description 生意转让列表
 * @author hdl
 * @date 2019/5/21
 */
class BusinessTransferListActivity : BaseActivity(), OnFilterClickListener {
    class LifePageDataSource @Inject constructor(apiService: ApiService) : PageDataSource<ResultBase<PageInfoBase<LifeBusinessTransferInfo>>, LifeBusinessTransferParams>(apiService) {
        override fun fetchData(loadType: Int, handler: IDataSource.IDataSourceResultHandler<ResultBase<PageInfoBase<LifeBusinessTransferInfo>>>?): Observable<ResultBase<PageInfoBase<LifeBusinessTransferInfo>>> {
            return apiService.getLifeBusinessTransferList(params).compose(RxUtil.normalSchedulers())
        }
    }

    @Inject
    lateinit var apiService: ApiService
    @Inject
    lateinit var iRefreshViewHolder: PtrRefreshViewHolder
    @Inject
    lateinit var dataSource: LifePageDataSource
    private lateinit var delegate: IDataDelegate
    lateinit var adapter: WealthCommonAdapter<LifeBusinessTransferInfo>
    lateinit var headerAdapter: RvHeaderFootViewAdapter<LifeBusinessTransferInfo>
    val params = LifeBusinessTransferParams()
    private lateinit var cityFilter: FilterListPopupWindow
    private lateinit var industryFilter: FilterListPopupWindow
    private lateinit var typeFilter: FilterListPopupWindow
    private lateinit var timeFilter: FilterListPopupWindow


    companion object {
        fun start(context: Context) {
            val starter = Intent(context, BusinessTransferListActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.activity_life_business_transfer_list)
        iRefreshViewHolder.init(this)
        registerBus()
        initView()
        initFilter()
    }

    override fun onDestroy() {
        super.onDestroy()
        unRegisterBus()
    }

    private fun initView() {
        topbar.setTopBarClickListener(this)

        adapter = WealthCommonAdapter(mContext, arrayListOf())
        headerAdapter = RvHeaderFootViewAdapter(adapter, mContext)
        list.adapter = headerAdapter
        list.layoutManager = LinearLayoutManager(mContext)
        list.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                super.getItemOffsets(outRect, view, parent, state)
                outRect.set(0, 0, 0, CommonUtil.dp2px(0.66f))
            }
        })

        dataSource.setParams(params)
        delegate = iRefreshViewHolder
                .setRefreshViewEnable(true)
                .setLoadMoreEnable(true)
                .setEmptyView(emptyview)
                .setDataAdapter(headerAdapter)
                .createDataDelegate()
        delegate.setDataSource(dataSource).refreshWithLoading()
    }

    private fun initFilter() {
        cityFilter = FilterListPopupWindow(mContext, 1, 7.5f)
        cityFilter.setOnFilterListener(this, 0)
        cityFilter.setData(CacheHelper.getInstance().getAllCityInfoListAndUnlimited("区域"), 0)

        val rentalMethodList = DbHelper.getInstance().getParentCodeListAndAll(C.ParentCode.INDUSTRY, "行业", "不限")
        industryFilter = FilterListPopupWindow(mContext, 1, getListFloat(rentalMethodList))
        industryFilter.setOnFilterListener(this, 1)
        industryFilter.setData(rentalMethodList, 0)

        val rentList = DbHelper.getInstance().getParentCodeListAndAll(C.ParentCode.SERVICE_TYPE, "类型", "不限")
        typeFilter = FilterListPopupWindow(mContext, 1, getListFloat(rentList))
        typeFilter.setOnFilterListener(this, 2)
        typeFilter.setData(rentList, 0)

        val timeList = ArrayList<ParentCodeInfo>()
        timeList.add(ParentCodeInfo("筛选", "不限", ""))
        timeList.add(ParentCodeInfo("最近3天", "1"))
        timeList.add(ParentCodeInfo("最近7天", "2"))
        timeList.add(ParentCodeInfo("最近一个月", "3"))
        timeList.add(ParentCodeInfo("最近三个月", "4"))
        timeFilter = FilterListPopupWindow(mContext, 1, getListFloat(timeList))
        timeFilter.setOnFilterListener(this, 3)
        timeFilter.setData(timeList, 0)

        fl_area.click {
            if(cityFilter.data?.isEmpty() != false){
                cityFilter.data = CacheHelper.getInstance().getAllCityInfoListAndUnlimited("区域")
            }
            cityFilter.show(pop_view_line)
            tv_area.isSelected = true
        }
        fl_industry.click {
            industryFilter.show(pop_view_line)
            tv_industry.isSelected = true
        }
        fl_type.click {
            typeFilter.show(pop_view_line)
            tv_type.isSelected = true
        }
        fl_filter.click {
            timeFilter.show(pop_view_line)
            tv_filter.isSelected = true
        }
    }

    override fun onPopItemSelected(filterInfo: FilterInfoUiModel?, requestCode: Int) {
        when(requestCode){
            0 -> {
                tv_area.text = filterInfo?.filterName
                if(filterInfo?.filterShowName == CacheHelper.CITY_UNLIMITED_STR){
                    params.city = ""
                }else {
                    params.city = filterInfo?.filterName
                }
            }
            1 -> {
                tv_industry.text = filterInfo?.filterName
                params.businessTransferType = filterInfo?.filterCode
            }
            2 -> {
                tv_type.text = filterInfo?.filterName
                params.serviceType = filterInfo?.filterCode
            }
            3 -> {
                tv_filter.text = filterInfo?.filterName
                params.filterUpdateTime = filterInfo?.filterCode
            }
        }
        delegate.refreshWithLoading()
    }

    override fun onPopWindowDismissed(requestCode: Int) {
        when(requestCode){
            0 -> {
                tv_area.isSelected = false
            }
            1 -> {
                tv_industry.isSelected = false
            }
            2 -> {
                tv_type.isSelected = false
            }
            3 -> {
                tv_filter.isSelected = false
            }
        }
    }

    override fun onTopBarRightImgClick() {
        if(isLoginToActivityByIsRelease) {
            ReleaseBusinessTransferActivity.start(mContext)
        }
    }

    /**
     * 刷新列表
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun updateLifeReleaseList(event: Event<String>) {
        if (C.EventKey.UPDATE_LIFE_RELEASE_LIST == event.key) {
            delegate.refreshWithLoading()
        }
    }

}