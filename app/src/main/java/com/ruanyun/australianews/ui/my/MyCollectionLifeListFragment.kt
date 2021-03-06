package com.ruanyun.australianews.ui.my

import android.graphics.Rect
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ruanyun.australianews.App
import com.ruanyun.australianews.R
import com.ruanyun.australianews.base.BaseFragment
import com.ruanyun.australianews.base.ResultBase
import com.ruanyun.australianews.base.refreshview.data.IDataDelegate
import com.ruanyun.australianews.base.refreshview.data.IDataSource
import com.ruanyun.australianews.base.refreshview.impl.PageDataSource
import com.ruanyun.australianews.base.refreshview.impl.PtrRefreshViewHolder
import com.ruanyun.australianews.base.refreshview.impl.RvHeaderFootViewAdapter
import com.ruanyun.australianews.data.ApiFailAction
import com.ruanyun.australianews.data.ApiManger
import com.ruanyun.australianews.data.ApiService
import com.ruanyun.australianews.data.ApiSuccessAction
import com.ruanyun.australianews.ext.clickWithTrigger
import com.ruanyun.australianews.model.LifeBrowseCollectionResultInfo
import com.ruanyun.australianews.model.params.CollectionBrowseNewsLifeListParams
import com.ruanyun.australianews.model.params.DeleteCollectionParams
import com.ruanyun.australianews.ui.adapter.BrowseCollectionLifeCommonAdapter
import com.ruanyun.australianews.util.CommonUtil
import com.ruanyun.australianews.util.RxUtil
import com.ruanyun.australianews.widget.TipDialog
import kotlinx.android.synthetic.main.fragment_collection_browse_life_list.*
import kotlinx.android.synthetic.main.layout_bottom_delete_management.*
import rx.Observable
import javax.inject.Inject

/**
 * @author hdl
 * @description 我的收藏生活服务列表
 * @date 2019/5/15
 */
class MyCollectionLifeListFragment : BaseFragment() {

    class NewsListPageDataSource @Inject constructor(apiService: ApiService) : PageDataSource<ResultBase<*>, CollectionBrowseNewsLifeListParams>(apiService) {
        override fun fetchData(loadType: Int, handler: IDataSource.IDataSourceResultHandler<ResultBase<*>>?): Observable<ResultBase<*>> {
            return apiService.getCollectionLifePageList(params).compose(RxUtil.normalSchedulers())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mContentView = inflater.inflate(R.layout.fragment_collection_browse_life_list, container, false)
        iRefreshViewHolder.init(mContentView)
        return mContentView
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    val adapter by lazy { BrowseCollectionLifeCommonAdapter(mContext, arrayListOf()) }
    @Inject
    lateinit var dataSource: NewsListPageDataSource
    @Inject
    lateinit var iRefreshViewHolder: PtrRefreshViewHolder
    private lateinit var delegate: IDataDelegate
    private val params = CollectionBrowseNewsLifeListParams()
    private lateinit var headerAdapter: RvHeaderFootViewAdapter<LifeBrowseCollectionResultInfo>
    private val tipDialog by lazy { TipDialog(mContext) }
    var deleteType = 2//1清空 2删除选中

    fun setEditMode(isEditMode: Boolean) {
        rl_delete_manage.visibility = if (isEditMode) View.VISIBLE else View.GONE
        if (!isEditMode) {
            adapter.datas.forEach { it.isSelect = false }
        }
        adapter.isEditMode = isEditMode
        adapter.notifyDataSetChanged()
        headerAdapter.notifyDataSetChanged()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
    }

    private fun initView() {
        headerAdapter = RvHeaderFootViewAdapter(adapter, mContext)
        list.adapter = headerAdapter
        list.layoutManager = LinearLayoutManager(mContext)
        list.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                super.getItemOffsets(outRect, view, parent, state)
                outRect.set(0, 0, 0, CommonUtil.dp2px(0.66f))
            }
        })
        params.userOid = App.getInstance().userOid
        params.model = 2//新闻
        dataSource.setParams(params)
        delegate = iRefreshViewHolder
                .setLoadMoreEnable(true)
                .setRefreshViewEnable(true)
                .setDataAdapter(headerAdapter)
                .setEmptyView(emptyview)
                .createDataDelegate()
        delegate.setDataSource(dataSource).refreshWithLoading()
        emptyview.setBackgroundColor(ContextCompat.getColor(mContext, R.color.white))
        tipDialog.setOkClick {
            tipDialog.dismiss()
            deleteCollectionInfo()
        }
        adapter.blockEmpty = {
            emptyview.showEmpty()
        }

        tv_clear_empty.clickWithTrigger {
            if (adapter.datas.isNotEmpty()) {
                deleteType = 1
//                tipDialog.show("温馨提示", "是否确认清空新闻收藏?", "确认")
                deleteCollectionInfo()
            }
        }
        tv_delete.clickWithTrigger {
            if (adapter.datas.any { it.isSelect }) {
                deleteType = 2
//                tipDialog.show("温馨提示", "是否确认删除选中新闻收藏?", "确认")
                deleteCollectionInfo()
            }
        }
    }

    /**
     * 删除收藏
     */
    private fun deleteCollectionInfo() {
        val params = DeleteCollectionParams()
        params.type = deleteType
        params.userOid = app.userOid
        params.model = 2
        if (deleteType == 2) {
            var collectionInfoOids = ""
            adapter.datas.forEach {
                if (it.isSelect) {
                    if (CommonUtil.isNotEmpty(collectionInfoOids)) {
                        collectionInfoOids += ","
                    }
                    collectionInfoOids += it.getLifeOrYellowPageCollectionBrowseOid()
                }
            }
            params.collectionInfoOids = collectionInfoOids
        }
        showLoadingView(R.string.in_submit)
        val subscription = ApiManger.getApiService().deleteCollectionInfo(params)
                .compose(RxUtil.normalSchedulers<ResultBase<*>>())
                .subscribe(object : ApiSuccessAction<ResultBase<*>>() {
                    override fun onSuccess(result: ResultBase<*>) {
                        disMissLoadingView()
                        showToast(result.msg)
                        delegate.refreshWithLoading()
                        (activity as MyCollectionActivity).onTopBarRightTextClick()
                    }

                    override fun onError(erroCode: Int, erroMsg: String) {
                        super.onError(erroCode, erroMsg)
                        disMissLoadingView()
                        showToast(erroMsg)
                    }
                }, object : ApiFailAction() {
                    override fun onFail(msg: String) {
                        super.onFail(msg)
                        disMissLoadingView()
                        showToast(msg)
                    }
                })
        addSubscrebe(subscription)
    }


}
