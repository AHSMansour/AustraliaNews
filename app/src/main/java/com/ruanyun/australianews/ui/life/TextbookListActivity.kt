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
import com.ruanyun.australianews.model.LifeTextbookInfo
import com.ruanyun.australianews.model.params.LifeTextbookParams
import com.ruanyun.australianews.ui.adapter.LifeReleaseCommonAdapter
import com.ruanyun.australianews.ui.life.release.ReleaseTextbookActivity
import com.ruanyun.australianews.util.*
import com.ruanyun.australianews.widget.LifeTextbookFilterPopWindow
import com.ruanyun.australianews.widget.filterpopwindow.FilterInfoUiModel
import com.ruanyun.australianews.widget.filterpopwindow.FilterListPopupWindow
import com.ruanyun.australianews.widget.filterpopwindow.OnFilterClickListener
import kotlinx.android.synthetic.main.activity_life_textbook_list.*
import kotlinx.android.synthetic.main.layout_refresh_rv_common.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import rx.Observable
import javax.inject.Inject

/**
 * @description 教科书列表
 * @author hdl
 * @date 2019/5/20
 */
class TextbookListActivity : BaseActivity(), OnFilterClickListener {
    class LifePageDataSource @Inject constructor(apiService: ApiService) : PageDataSource<ResultBase<PageInfoBase<LifeTextbookInfo>>, LifeTextbookParams>(apiService) {
        override fun fetchData(loadType: Int, handler: IDataSource.IDataSourceResultHandler<ResultBase<PageInfoBase<LifeTextbookInfo>>>?): Observable<ResultBase<PageInfoBase<LifeTextbookInfo>>> {
            return apiService.getLifeTextbookList(params).compose(RxUtil.normalSchedulers())
        }
    }

    @Inject
    lateinit var apiService: ApiService
    @Inject
    lateinit var iRefreshViewHolder: PtrRefreshViewHolder
    @Inject
    lateinit var dataSource: LifePageDataSource
    private lateinit var delegate: IDataDelegate
    lateinit var adapter: LifeReleaseCommonAdapter<Any?>
    lateinit var headerAdapter: RvHeaderFootViewAdapter<LifeTextbookInfo>
    val params = LifeTextbookParams()
    private lateinit var schoolFilter: FilterListPopupWindow
    private lateinit var typeFilter: FilterListPopupWindow
    private lateinit var formFilter: FilterListPopupWindow
    private val lifeTextbookFilterPopWindow by lazy { LifeTextbookFilterPopWindow(mContext) }


    companion object {
        fun start(context: Context) {
            val starter = Intent(context, TextbookListActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.activity_life_textbook_list)
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

        adapter = LifeReleaseCommonAdapter(mContext, arrayListOf())
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
        val schoolList = DbHelper.getInstance().getParentCodeListAndAll(C.ParentCode.BOOK_SCHOOL_NAME, "学校", "不限")
        schoolFilter = FilterListPopupWindow(mContext, 1, getListFloat(schoolList))
        schoolFilter.setOnFilterListener(this, 0)
        schoolFilter.setData(schoolList, 0)

        val rentalMethodList = DbHelper.getInstance().getParentCodeListAndAll(C.ParentCode.BOOK_TYPE, "类型", "不限")
        typeFilter = FilterListPopupWindow(mContext, 1, getListFloat(rentalMethodList))
        typeFilter.setOnFilterListener(this, 1)
        typeFilter.setData(rentalMethodList, 0)

        val rentList = DbHelper.getInstance().getParentCodeListAndAll(C.ParentCode.BOOK_SHAPE, "形式", "不限")
        formFilter = FilterListPopupWindow(mContext, 1, getListFloat(rentList))
        formFilter.setOnFilterListener(this, 2)
        formFilter.setData(rentList, 0)

        lifeTextbookFilterPopWindow.block = {oid1,oid2,oid3,oid4 ->
            params.newOldStandard = oid1
            params.isNotes = oid2
            params.transactionArea = oid3
            params.belongTo = oid4
            delegate.refreshWithLoading()
        }

        fl_school.click {
            schoolFilter.show(pop_view_line)
            tv_school.isSelected = true
        }
        fl_type.click {
            typeFilter.show(pop_view_line)
            tv_type.isSelected = true
        }
        fl_form.click {
            formFilter.show(pop_view_line)
            tv_form.isSelected = true
        }
        fl_filter.click {
            lifeTextbookFilterPopWindow.show(pop_view_line)
        }
    }

    override fun onPopItemSelected(filterInfo: FilterInfoUiModel?, requestCode: Int) {
        when(requestCode){
            0 -> {
                tv_school.text = filterInfo?.filterName
                params.school = filterInfo?.filterCode
            }
            1 -> {
                tv_type.text = filterInfo?.filterName
                params.bookeType = filterInfo?.filterCode
            }
            2 -> {
                tv_form.text = filterInfo?.filterName
                params.shape = filterInfo?.filterCode
            }
        }
        delegate.refreshWithLoading()
    }

    override fun onPopWindowDismissed(requestCode: Int) {
        when(requestCode){
            0 -> {
                tv_school.isSelected = false
            }
            1 -> {
                tv_type.isSelected = false
            }
            2 -> {
                tv_form.isSelected = false
            }
        }
    }

    override fun onTopBarRightImgClick() {
        if(isLoginToActivityByIsRelease) {
            ReleaseTextbookActivity.start(mContext)
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