$(function() {

	// init date tables
	var jobTable = $("#job_list").dataTable({
		"deferRender": true,
		"processing" : true,
	    "serverSide": true,
		"ajax": {
			url: base_url + "/jobinfo/pageList",
			type:"post",
	        data : function ( d ) {
	        	var obj = {};
	        	obj.jobGroup = $('#jobGroup').val();
                obj.triggerStatus = $('#triggerStatus').val();
                obj.jobDesc = $('#jobDesc').val();
	        	obj.executorHandler = $('#executorHandler').val();
                obj.author = $('#author').val();
	        	obj.start = d.start;
	        	obj.length = d.length;
                return obj;
            }
	    },
	    "searching": false,
	    "ordering": false,
	    //"scrollX": true,	// scroll x，close self-adaption
	    "columns": [
            {
                "data": null,  // 不绑定特定数据
                "defaultContent": '',  // 默认内容
                "bSortable": false,
                "visible": true,
                "width": '5%',
                "orderable": false,
                "className": 'dt-body-center dt-head-center',
                "render": function (data, type, row, meta) {
                    // 创建带有 id 的复选框
                    return '<input type="checkbox" class="row-checkbox" value="' + row.id + '">';
                }
            },
	                {
	                	"data": 'id',
	                	"visible" : true,
	                	"width":'5%'
	                },
	                {
	                	"data": 'jobGroup',
	                	"visible" : false,
	                	"render": function ( data, type, row ) {
	            			var groupMenu = $("#jobGroup").find("option");
	            			for ( var index in $("#jobGroup").find("option")) {
	            				if ($(groupMenu[index]).attr('value') == data) {
									return $(groupMenu[index]).html();
								}
							}
	            			return data;
	            		}
            		},
	                {
	                	"data": 'jobDesc',
						"visible" : true,
						"width":'23%'
					},
					{
						"data": 'scheduleType',
						"visible" : true,
						"width":'13%',
						"render": function ( data, type, row ) {
							if (row.scheduleConf) {
								return row.scheduleType + '：'+ row.scheduleConf;
							} else {
								return row.scheduleType;
							}
						}
					},
					{
						"data": 'glueType',
						"width":'24%',
						"visible" : true,
						"render": function ( data, type, row ) {
							var glueTypeTitle = findGlueTypeTitle(row.glueType);
                            if (row.executorHandler) {
                                return glueTypeTitle +"：" + row.executorHandler;
                            } else {
                                return glueTypeTitle;
                            }
						}
					},
	                { "data": 'executorParam', "visible" : false},
	                {
	                	"data": 'addTime',
	                	"visible" : false,
	                	"render": function ( data, type, row ) {
	                		return data?moment(new Date(data)).format("YYYY-MM-DD HH:mm:ss"):"";
	                	}
	                },
	                {
	                	"data": 'updateTime',
	                	"visible" : false,
	                	"render": function ( data, type, row ) {
	                		return data?moment(new Date(data)).format("YYYY-MM-DD HH:mm:ss"):"";
	                	}
	                },
	                { "data": 'author', "visible" : true, "width":'10%'},
	                { "data": 'alarmEmail', "visible" : false},
	                {
	                	"data": 'triggerStatus',
						"width":'10%',
	                	"visible" : true,
	                	"render": function ( data, type, row ) {
                            // status
                            if (1 == data) {
                                return '<small class="label label-success" >RUNNING</small>';
                            } else {
                                return '<small class="label label-default" >STOP</small>';
                            }
	                		return data;
	                	}
	                },
	                {
						"data": I18n.system_opt ,
						"width":'10%',
	                	"render": function ( data, type, row ) {
	                		return function(){

                                // status
                                var start_stop_div = "";
                                if (1 == row.triggerStatus ) {
                                    start_stop_div = '<li><a href="javascript:void(0);" class="job_operate" _type="job_pause" >'+ I18n.jobinfo_opt_stop +'</a></li>\n';
                                } else {
                                    start_stop_div = '<li><a href="javascript:void(0);" class="job_operate" _type="job_resume" >'+ I18n.jobinfo_opt_start +'</a></li>\n';
                                }

                                // job_next_time_html
								var job_next_time_html = '';
								if (row.scheduleType == 'CRON' || row.scheduleType == 'FIX_RATE') {
									job_next_time_html = '<li><a href="javascript:void(0);" class="job_next_time" >' + I18n.jobinfo_opt_next_time + '</a></li>\n';
								}

                                // log url
                                var logHref = base_url +'/joblog?jobId='+ row.id;

                                // code url
                                var codeBtn = "";
                                if ('BEAN' != row.glueType) {
                                    var codeUrl = base_url +'/jobcode?jobId='+ row.id;
                                    codeBtn = '<li><a href="'+ codeUrl +'" target="_blank" >GLUE IDE</a></li>\n';
                                    codeBtn += '<li class="divider"></li>\n';
                                }

                                // data
                                tableData['key'+row.id] = row;

                                // opt
                                var html = '<div class="btn-group">\n' +
                                    '     <button type="button" class="btn btn-primary btn-sm">'+ I18n.system_opt +'</button>\n' +
                                    '     <button type="button" class="btn btn-primary btn-sm dropdown-toggle" data-toggle="dropdown">\n' +
                                    '       <span class="caret"></span>\n' +
                                    '       <span class="sr-only">Toggle Dropdown</span>\n' +
                                    '     </button>\n' +
                                    '     <ul class="dropdown-menu" role="menu" _id="'+ row.id +'" >\n' +
                                    '       <li><a href="javascript:void(0);" class="job_trigger" >'+ I18n.jobinfo_opt_run +'</a></li>\n' +
                                    '       <li><a href="'+ logHref +'">'+ I18n.jobinfo_opt_log +'</a></li>\n' +
                                    '       <li><a href="javascript:void(0);" class="job_registryinfo" >' + I18n.jobinfo_opt_registryinfo + '</a></li>\n' +
									job_next_time_html +
                                    '       <li class="divider"></li>\n' +
                                    codeBtn +
                                    start_stop_div +
                                    '       <li><a href="javascript:void(0);" class="update" >'+ I18n.system_opt_edit +'</a></li>\n' +
                                    '       <li><a href="javascript:void(0);" class="job_operate" _type="job_del" >'+ I18n.system_opt_del +'</a></li>\n' +
									'       <li><a href="javascript:void(0);" class="job_copy" >'+ I18n.system_opt_copy +'</a></li>\n' +
                                    '     </ul>\n' +
                                    '   </div>';

	                			return html;
							};
	                	}
	                }
	            ],
		"language" : {
			"sProcessing" : I18n.dataTable_sProcessing ,
			"sLengthMenu" : I18n.dataTable_sLengthMenu ,
			"sZeroRecords" : I18n.dataTable_sZeroRecords ,
			"sInfo" : I18n.dataTable_sInfo ,
			"sInfoEmpty" : I18n.dataTable_sInfoEmpty ,
			"sInfoFiltered" : I18n.dataTable_sInfoFiltered ,
			"sInfoPostFix" : "",
			"sSearch" : I18n.dataTable_sSearch ,
			"sUrl" : "",
			"sEmptyTable" : I18n.dataTable_sEmptyTable ,
			"sLoadingRecords" : I18n.dataTable_sLoadingRecords ,
			"sInfoThousands" : ",",
			"oPaginate" : {
				"sFirst" : I18n.dataTable_sFirst ,
				"sPrevious" : I18n.dataTable_sPrevious ,
				"sNext" : I18n.dataTable_sNext ,
				"sLast" : I18n.dataTable_sLast
			},
			"oAria" : {
				"sSortAscending" : I18n.dataTable_sSortAscending ,
				"sSortDescending" : I18n.dataTable_sSortDescending
			}
		}
	});

    // table data
    var tableData = {};

    // ===== 子任务选择器：折叠摘要 + 展开 + 搜索添加 =====
    var CHILD_JOB_MAX_LEN = 100;    // 与 xxl_job_info.child_jobid varchar(100) 对齐
    var CHILD_JOB_OPTION_LIMIT = 50;
    var allJobsCache = null;        // [{id, jobDesc, jobGroup}]，页面会话内缓存

    function escapeHtml(str) {
        return $('<div>').text(str == null ? '' : String(str)).html();
    }

    function jobGroupTitle(groupId) {
        var title = '';
        $("#jobGroup").find("option").each(function () {
            if ($(this).attr('value') == groupId) {
                title = $(this).html();
                return false;
            }
        });
        return title;
    }

    function loadAllJobs(callback) {
        if (allJobsCache) { callback(allJobsCache); return; }
        $.ajax({
            url: base_url + '/jobinfo/pageList',
            type: 'POST',
            data: {
                start: 0, length: 3000,
                jobGroup: 0, triggerStatus: -1,     // 0 / -1 = 不过滤，取全部
                jobDesc: '', executorHandler: '', author: ''
            },
            success: function (res) {
                allJobsCache = [];
                var rows = (res && res.data) ? res.data : [];
                for (var i = 0; i < rows.length; i++) {
                    allJobsCache.push({
                        id: rows[i].id,
                        jobDesc: rows[i].jobDesc,
                        jobGroup: rows[i].jobGroup
                    });
                }
                callback(allJobsCache);
            },
            error: function () {
                allJobsCache = [];
                callback(allJobsCache);
            }
        });
    }

    function initChildJobPicker($picker) {
        var $hidden    = $picker.find("input[name='childJobId']");
        var $count     = $picker.find(".child-job-count");
        var $toggle    = $picker.find(".child-job-toggle");
        var $expanded  = $picker.find(".child-job-expanded");
        var $tags      = $picker.find(".child-job-tags");
        var $search    = $picker.find(".child-job-search");
        var $dropdown  = $picker.find(".child-job-dropdown");

        var selected = [];      // 有序 id 数组

        function findJob(id) {
            if (!allJobsCache) return null;
            for (var i = 0; i < allJobsCache.length; i++) {
                if (allJobsCache[i].id == id) return allJobsCache[i];
            }
            return null;
        }

        function refresh() {
            $hidden.val(selected.join(","));
            $count.text("已选 " + selected.length + " 个子任务");

            var html = "";
            for (var i = 0; i < selected.length; i++) {
                var job = findJob(selected[i]);
                var label = job ? (job.id + " " + escapeHtml(job.jobDesc)) : selected[i];
                html += '<span class="child-job-tag" data-id="' + selected[i] + '">'
                      + label + ' <i class="child-job-remove">×</i></span>';
            }
            $tags.html(html);
        }

        function addId(id) {
            if (selected.indexOf(id) >= 0) return;      // 已选，忽略
            if (selected.concat([id]).join(",").length > CHILD_JOB_MAX_LEN) {
                alert("子任务ID 总长度不能超过 " + CHILD_JOB_MAX_LEN + " 字符");
                return;
            }
            selected.push(id);
            refresh();
        }

        function removeId(id) {
            var idx = selected.indexOf(id);
            if (idx >= 0) { selected.splice(idx, 1); refresh(); }
        }

        // 展开 / 收起
        $toggle.on('click', function () {
            var willShow = $expanded.is(":hidden");
            $expanded.toggle(willShow);
            $toggle.text(willShow ? "收起 ▴" : "展开 ▾");
            if (willShow) {
                // 懒加载：展开时才拉任务列表，拿到后重绘标签（补上任务名）
                loadAllJobs(function () { refresh(); $search.trigger('focus'); });
            }
        });

        // 删除标签
        $tags.on('click', '.child-job-remove', function () {
            removeId(parseInt($(this).parent().attr('data-id'), 10));
        });

        // 搜索
        $search.on('input', function () {
            var kw = $.trim($(this).val()).toLowerCase();
            if (kw.length === 0) { $dropdown.hide().empty(); return; }
            loadAllJobs(function (jobs) {
                var matched = [];
                for (var i = 0; i < jobs.length && matched.length < CHILD_JOB_OPTION_LIMIT; i++) {
                    var job = jobs[i];
                    if (selected.indexOf(job.id) >= 0) continue;      // 已选的不用再列
                    if ((job.id + " " + job.jobDesc).toLowerCase().indexOf(kw) >= 0) {
                        matched.push(job);
                    }
                }
                var html = "";
                for (var j = 0; j < matched.length; j++) {
                    html += '<div class="child-job-option" data-id="' + matched[j].id + '">'
                          + '<span>' + escapeHtml(matched[j].id + "　" + matched[j].jobDesc) + '</span>'
                          + '<span class="child-job-option-group">' + escapeHtml(jobGroupTitle(matched[j].jobGroup)) + '</span>'
                          + '</div>';
                }
                if (html.length === 0) {
                    html = '<div class="child-job-empty">没有匹配的任务</div>';
                }
                $dropdown.html(html).show();
            });
        });

        // 点候选项加进标签
        $dropdown.on('click', '.child-job-option', function () {
            addId(parseInt($(this).attr('data-id'), 10));
            $search.val("").trigger('focus');
            $dropdown.hide().empty();
        });

        return {
            setValue: function (str) {
                selected = [];
                if (str) {
                    var parts = String(str).split(",");
                    for (var i = 0; i < parts.length; i++) {
                        var v = $.trim(parts[i]);
                        if (v.length > 0) selected.push(parseInt(v, 10));
                    }
                }
                refresh();
            },
            clear: function () {
                selected = [];
                $search.val("");
                $dropdown.hide().empty();
                $expanded.hide();
                $toggle.text("展开 ▾");
                refresh();
            }
        };
    }

    var addChildJobPicker = initChildJobPicker($("#addModal .child-job-picker"));
    var updateChildJobPicker = initChildJobPicker($("#updateModal .child-job-picker"));

    // 点选择器外面收起候选下拉
    $(document).on('click', function (e) {
        if (!$(e.target).closest('.child-job-picker').length) {
            $('.child-job-dropdown').hide();
        }
    });

    $('#checkBoxAll').on('click', function() {
        var $allCheckboxes = $('.row-checkbox');

        if ($allCheckboxes.length === 0) return;

        // 计算当前选中状态
        var allChecked = $allCheckboxes.length === $allCheckboxes.filter(':checked').length;
        var anyChecked = $allCheckboxes.filter(':checked').length > 0;

        // 决定新状态
        var newState = !(allChecked);

        // 设置所有复选框状态
        $allCheckboxes.prop('checked', newState).trigger('change');

        // 更新全选复选框状态
        $(this).prop('checked', newState);
        $(this).prop('indeterminate', false);
    });

    // 行复选框变化时更新全选状态
    $(document).on('change', '.row-checkbox', function() {
        updateSelectAllStatus();
    });

    // 更新全选复选框状态的函数
    function updateSelectAllStatus() {
        var $checkAll = $('#checkBoxAll');
        var $allCheckboxes = $('.row-checkbox');

        if ($allCheckboxes.length === 0) {
            $checkAll.prop('checked', false);
            $checkAll.prop('indeterminate', false);
            return;
        }

        var checkedCount = $allCheckboxes.filter(':checked').length;
        var totalCount = $allCheckboxes.length;
        console.log(
            checkedCount,totalCount
        )
        if(checkedCount>0){
            $('#batchOpeBtns').show();
        }else{
            $('#batchOpeBtns').hide();
        }
        // 使用 indeterminate 状态表示部分选中
        if (checkedCount === 0) {
            $checkAll.prop('checked', false);
            $checkAll.prop('indeterminate', false);
        } else if (checkedCount === totalCount) {
            $checkAll.prop('checked', true);
            $checkAll.prop('indeterminate', false);
        } else {
            $checkAll.prop('checked', false);
            $checkAll.prop('indeterminate', true);
        }
    }

    function getSelectedIds() {
        var ids = [];
        $('.row-checkbox:checked').each(function() {
            var id = $(this).val();
            if (id && id !== '') {
                ids.push(id);
            }
        });
        return ids;
    }
    // ==================== 批量停止按钮 ====================
    $('#batchStopBtn').on('click', function(e) {
        e.preventDefault();

        var selectedIds = getSelectedIds();
        console.log(JSON.stringify(selectedIds));
        if (selectedIds.length === 0) {
            alert('请至少选择一项进行操作');
            return;
        }

        if (!confirm(`确定要停止选中的 ${selectedIds.length} 项吗？`)) {
            return;
        }

        // 显示加载状态
        var $btn = $(this);
        var originalText = $btn.text();
        $btn.prop('disabled', true).text('处理中...');

        console.log(base_url)
        // 发送POST请求
        $.ajax({
            url: base_url + '/jobinfo/batchPause',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(selectedIds),
            headers: {
                // 'X-CSRF-TOKEN': $('meta[name="csrf-token"]').attr('content') // 如果使用CSRF保护
            },
            success: function(response) {
                if (response.code === 200) {
                    layer.msg(`成功停止 ${selectedIds.length} 项`);
                    // 可选：更新UI或刷新页面
                    jobTable.fnDraw();
                    $('#checkBoxAll').prop('checked', false).prop('indeterminate', false);
                } else {
                    alert('操作失败: ' + (response.message || '未知错误'));
                }
            },
            error: function(xhr, status, error) {
                console.error('批量停止失败:', error);
                alert('操作失败，请稍后重试');
            },
            complete: function() {
                $btn.prop('disabled', false).text(originalText);
            }
        });
    });

    // ==================== 批量启动按钮 ====================
    $('#batchStartBtn').on('click', function(e) {
        e.preventDefault();

        var selectedIds = getSelectedIds();
        if (selectedIds.length === 0) {
            alert('请至少选择一项进行操作');
            return;
        }

        if (!confirm(`确定要启动选中的 ${selectedIds.length} 项吗？`)) {
            return;
        }

        // 显示加载状态
        var $btn = $(this);
        var originalText = $btn.text();
        $btn.prop('disabled', true).text('处理中...');

        // 发送POST请求
        $.ajax({
            url: base_url + '/jobinfo/batchStart', // 替换为你的后端接口
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(selectedIds),
            headers: {
                // 'X-CSRF-TOKEN': $('meta[name="csrf-token"]').attr('content')
            },
            success: function(response) {
                if (response.code === 200) {
                    layer.msg(`成功启动 ${selectedIds.length} 项`);
                    jobTable.fnDraw();
                    $('#checkBoxAll').prop('checked', false).prop('indeterminate', false);
                } else {
                    alert('操作失败: ' + (response.message || '未知错误'));
                }
            },
            error: function(xhr, status, error) {
                console.error('批量启动失败:', error);
                alert('操作失败，请稍后重试');
            },
            complete: function() {
                $btn.prop('disabled', false).text(originalText);
            }
        });
    });

    // ==================== 批量删除按钮 ====================
    $('#batchDelBtn').on('click', function(e) {
        e.preventDefault();

        var selectedIds = getSelectedIds();
        if (selectedIds.length === 0) {
            alert('请至少选择一项进行操作');
            return;
        }

        if (!confirm(`确定要删除选中的 ${selectedIds.length} 项吗？此操作不可恢复！`)) {
            return;
        }

        // 显示加载状态
        var $btn = $(this);
        var originalText = $btn.text();
        $btn.prop('disabled', true).text('删除中...');

        // 发送POST请求
        $.ajax({
            url: base_url + '/jobinfo/batchRemove', // 替换为你的后端接口
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(selectedIds),
            headers: {
                // 'X-CSRF-TOKEN': $('meta[name="csrf-token"]').attr('content')
            },
            success: function(response) {
                if (response.code === 200) {
                    layer.msg(`成功删除 ${selectedIds.length} 项`);
                    jobTable.fnDraw();
                    $('#checkBoxAll').prop('checked', false).prop('indeterminate', false);
                } else {
                    alert('删除失败: ' + (response.message || '未知错误'));
                }
            },
            error: function(xhr, status, error) {
                console.error('批量删除失败:', error);
                alert('删除失败，请稍后重试');
            },
            complete: function() {
                $btn.prop('disabled', false).text(originalText);

            }
        });
    });



	// search btn
	$('#searchBtn').on('click', function(){
		jobTable.fnDraw();
	});

	// jobGroup change
	$('#jobGroup').on('change', function(){
        //reload
        var jobGroup = $('#jobGroup').val();
        window.location.href = base_url + "/jobinfo?jobGroup=" + jobGroup;
    });

	// job operate
	$("#job_list").on('click', '.job_operate',function() {
		var typeName;
		var url;
		var needFresh = false;

		var type = $(this).attr("_type");
		if ("job_pause" == type) {
			typeName = I18n.jobinfo_opt_stop ;
			url = base_url + "/jobinfo/stop";
			needFresh = true;
		} else if ("job_resume" == type) {
			typeName = I18n.jobinfo_opt_start ;
			url = base_url + "/jobinfo/start";
			needFresh = true;
		} else if ("job_del" == type) {
			typeName = I18n.system_opt_del ;
			url = base_url + "/jobinfo/remove";
			needFresh = true;
		} else {
			return;
		}

		var id = $(this).parents('ul').attr("_id");

		layer.confirm( I18n.system_ok + typeName + '?', {
			icon: 3,
			title: I18n.system_tips ,
            btn: [ I18n.system_ok, I18n.system_cancel ]
		}, function(index){
			layer.close(index);

			$.ajax({
				type : 'POST',
				url : url,
				data : {
					"id" : id
				},
				dataType : "json",
				success : function(data){
					if (data.code == 200) {
                        layer.msg( typeName + I18n.system_success );
                        if (needFresh) {
                            //window.location.reload();
                            jobTable.fnDraw(false);
                        }
					} else {
                        layer.msg( data.msg || typeName + I18n.system_fail );
					}
				}
			});
		});
	});

    // job trigger
    $("#job_list").on('click', '.job_trigger',function() {
        var id = $(this).parents('ul').attr("_id");
        var row = tableData['key'+id];

        $("#jobTriggerModal .form input[name='id']").val( row.id );
        $("#jobTriggerModal .form textarea[name='executorParam']").val( row.executorParam );

        $('#jobTriggerModal').modal({backdrop: false, keyboard: false}).modal('show');
    });
    $("#jobTriggerModal .ok").on('click',function() {
        $.ajax({
            type : 'POST',
            url : base_url + "/jobinfo/trigger",
            data : {
                "id" : $("#jobTriggerModal .form input[name='id']").val(),
                "executorParam" : $("#jobTriggerModal .textarea[name='executorParam']").val(),
				"addressList" : $("#jobTriggerModal .textarea[name='addressList']").val()
            },
            dataType : "json",
            success : function(data){
                if (data.code == 200) {
                    $('#jobTriggerModal').modal('hide');

                    layer.msg( I18n.jobinfo_opt_run + I18n.system_success );
                } else {
                    layer.msg( data.msg || I18n.jobinfo_opt_run + I18n.system_fail );
                }
            }
        });
    });
    $("#jobTriggerModal").on('hide.bs.modal', function () {
        $("#jobTriggerModal .form")[0].reset();
    });


    // job registryinfo
    $("#job_list").on('click', '.job_registryinfo',function() {
        var id = $(this).parents('ul').attr("_id");
        var row = tableData['key'+id];

        var jobGroup = row.jobGroup;

        $.ajax({
            type : 'POST',
            url : base_url + "/jobgroup/loadById",
            data : {
                "id" : jobGroup
            },
            dataType : "json",
            success : function(data){

                var html = '<div>';
                if (data.code == 200 && data.content.registryList) {
                    for (var index in data.content.registryList) {
                        html += (parseInt(index)+1) + '. <span class="badge bg-green" >' + data.content.registryList[index] + '</span><br>';
                    }
                }
                html += '</div>';

                layer.open({
                    title: I18n.jobinfo_opt_registryinfo ,
                    btn: [ I18n.system_ok ],
                    content: html
                });

            }
        });

    });

    // job_next_time
    $("#job_list").on('click', '.job_next_time',function() {
        var id = $(this).parents('ul').attr("_id");
        var row = tableData['key'+id];

        $.ajax({
            type : 'POST',
            url : base_url + "/jobinfo/nextTriggerTime",
            data : {
                "scheduleType" : row.scheduleType,
				"scheduleConf" : row.scheduleConf
            },
            dataType : "json",
            success : function(data){

            	if (data.code != 200) {
                    layer.open({
                        title: I18n.jobinfo_opt_next_time ,
                        btn: [ I18n.system_ok ],
                        content: data.msg
                    });
				} else {
                    var html = '<center>';
                    if (data.code == 200 && data.content) {
                        for (var index in data.content) {
                            html += '<span>' + data.content[index] + '</span><br>';
                        }
                    }
                    html += '</center>';

                    layer.open({
                        title: I18n.jobinfo_opt_next_time ,
                        btn: [ I18n.system_ok ],
                        content: html
                    });
				}

            }
        });

    });

	// add
	$(".add").click(function(){

		// init-cronGen
        $("#addModal .form input[name='schedule_conf_CRON']").show().siblings().remove();
        $("#addModal .form input[name='schedule_conf_CRON']").cronGen({});

		// 》init scheduleType
		$("#updateModal .form select[name=scheduleType]").change();

		// 》init glueType
		$("#updateModal .form select[name=glueType]").change();

		$('#addModal').modal({backdrop: false, keyboard: false}).modal('show');
	});
	var addModalValidate = $("#addModal .form").validate({
		errorElement : 'span',
        errorClass : 'help-block',
        focusInvalid : true,
        rules : {
			jobDesc : {
				required : true,
				maxlength: 50
			},
			author : {
				required : true
			}/*,
            executorTimeout : {
                digits:true
            },
            executorFailRetryCount : {
                digits:true
            }*/
        },
        messages : {
            jobDesc : {
            	required : I18n.system_please_input + I18n.jobinfo_field_jobdesc
            },
            author : {
            	required : I18n.system_please_input + I18n.jobinfo_field_author
            }/*,
            executorTimeout : {
                digits: I18n.system_please_input + I18n.system_digits
            },
            executorFailRetryCount : {
                digits: I18n.system_please_input + I18n.system_digits
            }*/
        },
		highlight : function(element) {
            $(element).closest('.form-group').addClass('has-error');
        },
        success : function(label) {
            label.closest('.form-group').removeClass('has-error');
            label.remove();
        },
        errorPlacement : function(error, element) {
            element.parent('div').append(error);
        },
        submitHandler : function(form) {

			// process executorTimeout+executorFailRetryCount
            var executorTimeout = $("#addModal .form input[name='executorTimeout']").val();
            if(!/^\d+$/.test(executorTimeout)) {
                executorTimeout = 0;
			}
            $("#addModal .form input[name='executorTimeout']").val(executorTimeout);
            var executorFailRetryCount = $("#addModal .form input[name='executorFailRetryCount']").val();
            if(!/^\d+$/.test(executorFailRetryCount)) {
                executorFailRetryCount = 0;
            }
            $("#addModal .form input[name='executorFailRetryCount']").val(executorFailRetryCount);

            // process schedule_conf
			var scheduleType = $("#addModal .form select[name='scheduleType']").val();
			var scheduleConf;
			if (scheduleType == 'CRON') {
				scheduleConf = $("#addModal .form input[name='cronGen_display']").val();
			} else if (scheduleType == 'FIX_RATE') {
				scheduleConf = $("#addModal .form input[name='schedule_conf_FIX_RATE']").val();
			} else if (scheduleType == 'FIX_DELAY') {
				scheduleConf = $("#addModal .form input[name='schedule_conf_FIX_DELAY']").val();
			}
			$("#addModal .form input[name='scheduleConf']").val( scheduleConf );

        	$.post(base_url + "/jobinfo/add",  $("#addModal .form").serialize(), function(data, status) {
    			if (data.code == "200") {
					$('#addModal').modal('hide');
					layer.open({
						title: I18n.system_tips ,
                        btn: [ I18n.system_ok ],
						content: I18n.system_add_suc ,
						icon: '1',
						end: function(layero, index){
							jobTable.fnDraw();
							//window.location.reload();
						}
					});
    			} else {
					layer.open({
						title: I18n.system_tips ,
                        btn: [ I18n.system_ok ],
						content: (data.msg || I18n.system_add_fail),
						icon: '2'
					});
    			}
    		});
		}
	});
	$("#addModal").on('hide.bs.modal', function () {
        addModalValidate.resetForm();
		$("#addModal .form")[0].reset();
		addChildJobPicker.clear();
		$("#addModal .form .form-group").removeClass("has-error");
		$(".remote_panel").show();	// remote

		$("#addModal .form input[name='executorHandler']").removeAttr("readonly");
	});

	// scheduleType change
	$(".scheduleType").change(function(){
		var scheduleType = $(this).val();
		$(this).parents("form").find(".schedule_conf").hide();
		$(this).parents("form").find(".schedule_conf_" + scheduleType).show();

	});

    // glueType change
    $(".glueType").change(function(){
		// executorHandler
        var $executorHandler = $(this).parents("form").find("input[name='executorHandler']");
        var glueType = $(this).val();
        if ('BEAN' != glueType) {
            $executorHandler.val("");
            $executorHandler.attr("readonly","readonly");
        } else {
            $executorHandler.removeAttr("readonly");
        }
    });

	$("#addModal .glueType").change(function(){
		// glueSource
		var glueType = $(this).val();
		if ('GLUE_GROOVY'==glueType){
			$("#addModal .form textarea[name='glueSource']").val( $("#addModal .form .glueSource_java").val() );
		} else if ('GLUE_SHELL'==glueType){
			$("#addModal .form textarea[name='glueSource']").val( $("#addModal .form .glueSource_shell").val() );
		} else if ('GLUE_PYTHON'==glueType){
			$("#addModal .form textarea[name='glueSource']").val( $("#addModal .form .glueSource_python").val() );
		} else if ('GLUE_PHP'==glueType){
            $("#addModal .form textarea[name='glueSource']").val( $("#addModal .form .glueSource_php").val() );
        } else if ('GLUE_NODEJS'==glueType){
			$("#addModal .form textarea[name='glueSource']").val( $("#addModal .form .glueSource_nodejs").val() );
		} else if ('GLUE_POWERSHELL'==glueType){
            $("#addModal .form textarea[name='glueSource']").val( $("#addModal .form .glueSource_powershell").val() );
        } else {
            $("#addModal .form textarea[name='glueSource']").val("");
		}
	});

	// update
	$("#job_list").on('click', '.update',function() {

        var id = $(this).parents('ul').attr("_id");
        var row = tableData['key'+id];

		// fill base
		$("#updateModal .form input[name='id']").val( row.id );
		$('#updateModal .form select[name=jobGroup] option[value='+ row.jobGroup +']').prop('selected', true);
		$("#updateModal .form input[name='jobDesc']").val( row.jobDesc );
		$("#updateModal .form input[name='author']").val( row.author );
		$("#updateModal .form input[name='alarmEmail']").val( row.alarmEmail );

		// fill trigger
		$('#updateModal .form select[name=scheduleType] option[value='+ row.scheduleType +']').prop('selected', true);
		$("#updateModal .form input[name='scheduleConf']").val( row.scheduleConf );
		if (row.scheduleType == 'CRON') {
			$("#updateModal .form input[name='schedule_conf_CRON']").val( row.scheduleConf );
		} else if (row.scheduleType == 'FIX_RATE') {
			$("#updateModal .form input[name='schedule_conf_FIX_RATE']").val( row.scheduleConf );
		} else if (row.scheduleType == 'FIX_DELAY') {
			$("#updateModal .form input[name='schedule_conf_FIX_DELAY']").val( row.scheduleConf );
		}

		// 》init scheduleType
		$("#updateModal .form select[name=scheduleType]").change();

		// fill job
		$('#updateModal .form select[name=glueType] option[value='+ row.glueType +']').prop('selected', true);
		$("#updateModal .form input[name='executorHandler']").val( row.executorHandler );
		$("#updateModal .form textarea[name='executorParam']").val( row.executorParam );

		// 》init glueType
		$("#updateModal .form select[name=glueType]").change();

		// 》init-cronGen
		$("#updateModal .form input[name='schedule_conf_CRON']").show().siblings().remove();
		$("#updateModal .form input[name='schedule_conf_CRON']").cronGen({});

		// fill advanced
		$('#updateModal .form select[name=executorRouteStrategy] option[value='+ row.executorRouteStrategy +']').prop('selected', true);
		$("#updateModal .form input[name='childJobId']").val( row.childJobId );
		updateChildJobPicker.setValue( row.childJobId );
		$('#updateModal .form select[name=misfireStrategy] option[value='+ row.misfireStrategy +']').prop('selected', true);
		$('#updateModal .form select[name=executorBlockStrategy] option[value='+ row.executorBlockStrategy +']').prop('selected', true);
		$("#updateModal .form input[name='executorTimeout']").val( row.executorTimeout );
        $("#updateModal .form input[name='executorFailRetryCount']").val( row.executorFailRetryCount );

		// show
		$('#updateModal').modal({backdrop: false, keyboard: false}).modal('show');
	});
	var updateModalValidate = $("#updateModal .form").validate({
		errorElement : 'span',
        errorClass : 'help-block',
        focusInvalid : true,

		rules : {
			jobDesc : {
				required : true,
				maxlength: 50
			},
			author : {
				required : true
			}
		},
		messages : {
			jobDesc : {
                required : I18n.system_please_input + I18n.jobinfo_field_jobdesc
			},
			author : {
				required : I18n.system_please_input + I18n.jobinfo_field_author
			}
		},
		highlight : function(element) {
            $(element).closest('.form-group').addClass('has-error');
        },
        success : function(label) {
            label.closest('.form-group').removeClass('has-error');
            label.remove();
        },
        errorPlacement : function(error, element) {
            element.parent('div').append(error);
        },
        submitHandler : function(form) {

            // process executorTimeout + executorFailRetryCount
            var executorTimeout = $("#updateModal .form input[name='executorTimeout']").val();
            if(!/^\d+$/.test(executorTimeout)) {
                executorTimeout = 0;
            }
            $("#updateModal .form input[name='executorTimeout']").val(executorTimeout);
            var executorFailRetryCount = $("#updateModal .form input[name='executorFailRetryCount']").val();
            if(!/^\d+$/.test(executorFailRetryCount)) {
                executorFailRetryCount = 0;
            }
            $("#updateModal .form input[name='executorFailRetryCount']").val(executorFailRetryCount);


			// process schedule_conf
			var scheduleType = $("#updateModal .form select[name='scheduleType']").val();
			var scheduleConf;
			if (scheduleType == 'CRON') {
				scheduleConf = $("#updateModal .form input[name='cronGen_display']").val();
			} else if (scheduleType == 'FIX_RATE') {
				scheduleConf = $("#updateModal .form input[name='schedule_conf_FIX_RATE']").val();
			} else if (scheduleType == 'FIX_DELAY') {
				scheduleConf = $("#updateModal .form input[name='schedule_conf_FIX_DELAY']").val();
			}
			$("#updateModal .form input[name='scheduleConf']").val( scheduleConf );

			// post
    		$.post(base_url + "/jobinfo/update", $("#updateModal .form").serialize(), function(data, status) {
    			if (data.code == "200") {
					$('#updateModal').modal('hide');
					layer.open({
						title: I18n.system_tips ,
                        btn: [ I18n.system_ok ],
						content: I18n.system_update_suc ,
						icon: '1',
						end: function(layero, index){
							//window.location.reload();
							jobTable.fnDraw();
						}
					});
    			} else {
					layer.open({
						title: I18n.system_tips ,
                        btn: [ I18n.system_ok ],
						content: (data.msg || I18n.system_update_fail ),
						icon: '2'
					});
    			}
    		});
		}
	});
	$("#updateModal").on('hide.bs.modal', function () {
        updateModalValidate.resetForm();
        $("#updateModal .form")[0].reset();
        updateChildJobPicker.clear();
        $("#updateModal .form .form-group").removeClass("has-error");
	});

    /**
	 * find title by name, GlueType
     */
	function findGlueTypeTitle(glueType) {
		var glueTypeTitle;
        $("#addModal .form select[name=glueType] option").each(function () {
            var name = $(this).val();
            var title = $(this).text();
            if (glueType == name) {
                glueTypeTitle = title;
                return false
            }
        });
        return glueTypeTitle;
    }

    // job_copy
	$("#job_list").on('click', '.job_copy',function() {

		var id = $(this).parents('ul').attr("_id");
		var row = tableData['key'+id];

		// fill base
		$('#addModal .form select[name=jobGroup] option[value='+ row.jobGroup +']').prop('selected', true);
		$("#addModal .form input[name='jobDesc']").val( row.jobDesc );
		$("#addModal .form input[name='author']").val( row.author );
		$("#addModal .form input[name='alarmEmail']").val( row.alarmEmail );

		// fill trigger
		$('#addModal .form select[name=scheduleType] option[value='+ row.scheduleType +']').prop('selected', true);
		$("#addModal .form input[name='scheduleConf']").val( row.scheduleConf );
		if (row.scheduleType == 'CRON') {
			$("#addModal .form input[name='schedule_conf_CRON']").val( row.scheduleConf );
		} else if (row.scheduleType == 'FIX_RATE') {
			$("#addModal .form input[name='schedule_conf_FIX_RATE']").val( row.scheduleConf );
		} else if (row.scheduleType == 'FIX_DELAY') {
			$("#addModal .form input[name='schedule_conf_FIX_DELAY']").val( row.scheduleConf );
		}

		// 》init scheduleType
		$("#addModal .form select[name=scheduleType]").change();

		// fill job
		$('#addModal .form select[name=glueType] option[value='+ row.glueType +']').prop('selected', true);
		$("#addModal .form input[name='executorHandler']").val( row.executorHandler );
		$("#addModal .form textarea[name='executorParam']").val( row.executorParam );

		// 》init glueType
		$("#addModal .form select[name=glueType]").change();

		// 》init-cronGen
		$("#addModal .form input[name='schedule_conf_CRON']").show().siblings().remove();
		$("#addModal .form input[name='schedule_conf_CRON']").cronGen({});

		// fill advanced
		$('#addModal .form select[name=executorRouteStrategy] option[value='+ row.executorRouteStrategy +']').prop('selected', true);
		$("#addModal .form input[name='childJobId']").val( row.childJobId );
		addChildJobPicker.setValue( row.childJobId );
		$('#addModal .form select[name=misfireStrategy] option[value='+ row.misfireStrategy +']').prop('selected', true);
		$('#addModal .form select[name=executorBlockStrategy] option[value='+ row.executorBlockStrategy +']').prop('selected', true);
		$("#addModal .form input[name='executorTimeout']").val( row.executorTimeout );
		$("#addModal .form input[name='executorFailRetryCount']").val( row.executorFailRetryCount );

		// show
		$('#addModal').modal({backdrop: false, keyboard: false}).modal('show');
	});

});
