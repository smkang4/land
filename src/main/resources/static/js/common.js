
    function showLoadingModal() {

        // 기존 모달 제거 (중복 생성 방지)
        $('#dynamicLoadingModal').remove();

       // 모달 HTML 생성
       var modalHTML = `
           <div class="modal fade" id="dynamicLoadingModal" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-9999" aria-labelledby="dynamicLoadingModalLabel" aria-hidden="true">
               <div class="modal-dialog modal-dialog-centered">
                   <div class="modal-content square-modal">
                       <div class="modal-body d-flex flex-column justify-content-center align-items-center">
                           <div class="spinner-border text-primary" role="status">
                               <span class="sr-only"></span>
                           </div>
                           <p class="mt-3">Please wait...</p>
                       </div>
                   </div>
               </div>
           </div>
       `;


        // 모달 HTML을 body에 추가
        $('body').append(modalHTML);

        // 모달 표시
//        $('#dynamicLoadingModal').modal({
//            backdrop: 'static',
//            keyboard: false
//        });



        $('#dynamicLoadingModal').modal('show');
    }

    function hideLoadingModal() {

        setTimeout(function(){
            // 모달 숨기기
            console.log("modal hide");
            $('#dynamicLoadingModal').modal('hide');

          // 모달이 완전히 숨겨진 후 제거
            $('#dynamicLoadingModal').on('hidden.bs.modal', function (e) {
                $(this).remove();
                $('body').removeClass('modal-open');
                $('.modal-backdrop').remove();
            });

        },500);


    }

    $("select").select2({
       theme: "bootstrap-5",
        containerCssClass: "select2--small", // For Select2 v4.0
        selectionCssClass: "select2--small", // For Select2 v4.1
        dropdownCssClass: "select2--small",
        placeholder: "Select a state",
        allowClear: true,
        closeOnSelect : true,
    });

    function ufn_select2(element,vPlaceH){
        $(element).select2({
           theme: "bootstrap-5",
            containerCssClass: "select2--small", // For Select2 v4.0
            selectionCssClass: "select2--small", // For Select2 v4.1
            dropdownCssClass: "select2--small",
            placeholder: vPlaceH,
            allowClear: true,
            closeOnSelect : true,
        });
    }
    function ufn_select2_m(element,vPlaceH,vChangeVal,onChange){
        $(element).select2({
           theme: "bootstrap-5",
//            containerCssClass: "select2--small", // For Select2 v4.0
//            selectionCssClass: "select2--small", // For Select2 v4.1
//            dropdownCssClass: "select2--small",
            placeholder: vPlaceH,
            allowClear: true,
            closeOnSelect : true,
        });

        $(element).on("change",function(){
            var id = $(this).val();
            if(onChange){
                onChange(id);
            }
        });

        $(element).val(vChangeVal).trigger('change');
    }

    $('select').on('select2:open', function() {
        $(".select2-search__field")[0].focus();
    });

    function ufn_select2_getData(vUrl,element,vPlaceH,vChangeVal,onSelect,onChange){
        $(element).empty();
        $.ajax({
            url: vUrl,
            dataType: "json",
            contentType: "application/json; UTF-8;",
            success: function(data) {
                //data = JSON.parse(data);
                $(element).prepend('<option selected=""></option>').select2({
                    allowClear: true,
                    theme: "bootstrap-5",
                    placeholder: vPlaceH,
                    data: data,
                });
                $(element).val(vChangeVal).trigger('change');
            }
        });
        $(element).on('select2:select', function (e) {
            if(onSelect){
                onSelect(e.params.data);
            }
        }).on("change",function(){
            var id = $(this).val();
            if(onChange){
                onChange(id);
            }
        });

    }

    // 그리드ID, GET 주소, 폼 아이디
    function G_load(VGrid,vUrl,vFormId){
        showLoadingModal();
        var params = $("#"+vFormId).serialize();
        $.ajax({
            url : vUrl,
            method :"get",
            async : true,
            contentType: "application/json; UTF-8;",
            dataType : "JSON",
            data : params,
            success : function(result){
                VGrid.resetData(result);
                console.log(result);
                console.log("성공");
            }
            ,beforeSend:function(){
                //(이미지 보여주기 처리)
                $('.btn_s').attr("disabled", true);
                VGrid.clear();
            }
            ,complete:function(result){
                console.log("loading ...complete");
                hideLoadingModal();
                //(이미지 감추기 처리)
                $.each(result.responseJSON, function(index, item){
                    if(item.cont_stat=="계약종료"){
                        VGrid.addCellClassName(index, 'cont_stat', 'custom-txt-color');
                    }
                });
                $('.btn_s').attr("disabled", false);
            }
            ,error:function(e){
                //조회 실패일 때 처리
            }
        });
    }

    class RowNumberRenderer {
        constructor(props) {
            const el = document.createElement('span');
            el.innerHTML = `No.${props.formattedValue}`;
            this.el = el;
        }

        getElement() {
            return this.el;
        }

        render(props) {
            this.el.innerHTML = `No.${props.formattedValue}`;
        }
    }

    class CheckboxRenderer {
        constructor(props) {
            const { grid, rowKey } = props;

            const label = document.createElement('label');
            label.className = 'checkbox tui-grid-row-header-checkbox';
            label.setAttribute('for', String(rowKey));

            const hiddenInput = document.createElement('input');
            hiddenInput.className = 'hidden-input';
            hiddenInput.id = String(rowKey);

            const customInput = document.createElement('span');
            customInput.className = 'custom-input';

            label.appendChild(hiddenInput);
            label.appendChild(customInput);

            hiddenInput.type = 'checkbox';
            label.addEventListener('click', (ev) => {
              ev.preventDefault();

              if (hiddenInput.disabled) {
                  console.log("disabled");
                  return; // 비활성화된 체크박스는 클릭 이벤트를 처리하지 않음
              }

              if (ev.shiftKey) {
                grid[!hiddenInput.checked ? 'checkBetween' : 'uncheckBetween'](rowKey);
                return;
              }

              grid[!hiddenInput.checked ? 'check' : 'uncheck'](rowKey);
            });

            this.el = label;

            this.render(props);
        }

        getElement() {
            return this.el;
        }

        render(props) {
            const hiddenInput = this.el.querySelector('.hidden-input');
            const checked = Boolean(props.value);

            hiddenInput.checked = checked;
        }
    }


 function ufn_comma(num) {
    var strNum = String(num);
    return strNum.replace(/(\d)(?=(?:\d{3})+(?!\d))/g, '$1,');
}

function ufn_removeCommas(text) {
    // 쉼표 제거
    return text.replace(/,/g, '');
}

function birthFormatter(num){
	if(!num){
		return "";
	}
	var formatNum = '';
	num=num.replace(/\s/gi, "");
	if(num.length == 8){
		formatNum = num.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3');
	}else{
		formatNum = num;
	}
	return formatNum;
}

    function phoneFormatter(num) {
        var formatNum = '';
        try{
            if (num.length == 11) {
                formatNum = num.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
            } else if (num.length == 8) {
                formatNum = num.replace(/(\d{4})(\d{4})/, '$1-$2');
            } else {
                if (num.indexOf('02') == 0) {
                    formatNum = num.replace(/(\d{2})(\d{4})(\d{4})/, '$1-$2-$3');
                } else {
                    formatNum = num.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3');
                }
            }
        } catch(e) {
            formatNum = num;
        }
        return formatNum;
    }

    function ufn_transaction(url,type,data,data_type,f_beforeSend,f_success,f_error,success_msg){

        showLoadingModal();

        $.ajax({
            url: url, // 요청 할 주소
            async: true, // false 일 경우 동기 요청으로 변경
            type: type, // GET, PUT
            data: data, // 전송할 데이터
            dataType: (data_type == "") ? "text" : data_type, // xml, json, script, html
            beforeSend: function(jqXHR) {
                f_beforeSend(jqXHR);
            },
            success: function(data) {
                if(success_msg != ""){
                    MsgBox.Alert(success_msg,function(){
                        f_success(data);
                    });
                }else{
                    f_success(data);
                }
                //console.log(data);
             }, // 요청 완료 시
            error: function(jqXHR, textStatus, errorThrown) {
                f_error(jqXHR);
                console.log("요청 실패:");
                console.log("상태:", textStatus);
                console.log("에러:", errorThrown);
            },
            complete: function(jqXHR, textStatus) {
                hideLoadingModal(); // 로딩 모달 숨기기
                console.log("요청 완료:", textStatus);
            }
        });

    }

    // 파일 ufn_transaction 함수
    function ufn_transaction_file(url, type, data, data_type, f_beforeSend, f_success, f_error, success_msg) {

        showLoadingModal();

        $.ajax({
            url: url, // 요청 할 주소
            async: true, // false 일 경우 동기 요청으로 변경
            type: type, // GET, PUT
            data: data, // 전송할 데이터
            xhrFields: {
                responseType: 'blob' // 응답 타입을 blob으로 설정
            },
            beforeSend: function(jqXHR) {
                f_beforeSend(jqXHR);
            },
            success: function(data, textStatus, jqXHR) {
                if(success_msg !== ""){
                    MsgBox.Alert(success_msg, function(){
                        f_success(data, jqXHR);
                    });
                }else{
                    f_success(data, jqXHR);
                }
            }, // 요청 완료 시
            error: function(jqXHR, textStatus, errorThrown) {
                f_error(jqXHR);
                console.log("요청 실패:");
                console.log("상태:", textStatus);
                console.log("에러:", errorThrown);
            },
            complete: function(jqXHR, textStatus) {
                hideLoadingModal(); // 로딩 모달 숨기기
                console.log("요청 완료:", textStatus);
            }
        });
    }


//    var MsgBox = {
//        Alert: function(msg, okhandler) {
//            new Promise((resolve, reject) => {
//                $("#msg_popup #btn_confirm").hide();
//                $("#msg_popup #btn_alert").show();
//
//                $("#msg_popup #alert_ok").unbind();
//                $("#msg_popup .modal-body").html(msg);
//                $('#msg_popup').modal('show');
//
//                $("#msg_popup #alert_ok").click(function() {
//                    $('#msg_popup').modal('hide');
//                });
//
//                $("#msg_popup").on("hidden.bs.modal", function(e) {
//                    e.stopPropagation();
//                    if(okhandler != null) resolve();
//                    else reject();
//                });
//            }).then(okhandler).catch(function() {});
//        },
//
//        /* Confirm */
//        Confirm: function(msg, yeshandler, nohandler) {
//            new Promise((resolve, reject) => {
//                var flag = false;
//                $("#msg_popup #btn_alert").hide();
//                $("#msg_popup #btn_confirm").show();
//
//                $("#msg_popup #confirm_yes").unbind();
//                $("#msg_popup #confirm_no").unbind();
//                $("#msg_popup .modal-body").html(msg);
//                $('#msg_popup').modal('show');
//
//                $('#msg_popup').on('keypress', function (e) {
//                    var keycode = (e.keyCode ? e.keyCode : e.which);
//                    if(keycode == '13') {
//                        flag = true;
//                        $('#msg_popup').modal('hide');
//                    }
//                });
//
//                $("#msg_popup #confirm_yes").click(function() {
//                    flag = true;
//                    $('#msg_popup').modal('hide');
//
//                });
//                $("#msg_popup #confirm_no").click(function() {
//                    flag = false;
//                    $('#msg_popup').modal('hide');
//                });
//
//                $("#msg_popup").on("hidden.bs.modal", function(e) {
//                    e.stopPropagation();
//                    if(yeshandler != null && flag == true){
//                        resolve(1);
//                    }
//                    else if(nohandler != null && flag == false){
//                        resolve(2);
//                    }
//                    else reject();
//                });
//            }).then(function(value) {
//                if(value == 1){
//                    yeshandler();
//                }else if(value == 2){
//                    nohandler();
//                }
//            }).catch(function() {});
//        },
//    }

    var MsgBox = {
      Alert: function(msg, okhandler) {
        return new Promise((resolve, reject) => {
          $("#msg_popup #btn_confirm").hide();
          $("#msg_popup #btn_alert").show();

          $("#msg_popup #alert_ok").unbind();
          $("#msg_popup .modal-body").html(msg);
          $('#msg_popup').modal('show');

          $("#msg_popup #alert_ok").click(function() {
            $('#msg_popup').modal('hide');
          });

          $("#msg_popup").on("hidden.bs.modal", function(e) {
            e.stopPropagation();
            if(okhandler != null) resolve();
            else reject();
          });
        }).then(okhandler).catch(function() {});
      },

      Confirm: function(msg, yeshandler, nohandler) {
        return new Promise((resolve, reject) => {
          var flag = false;
          $("#msg_popup #btn_alert").hide();
          $("#msg_popup #btn_confirm").show();

          $("#msg_popup #confirm_yes").unbind();
          $("#msg_popup #confirm_no").unbind();
          $("#msg_popup .modal-body").html(msg);
          $('#msg_popup').modal('show');

          $('#msg_popup').on('keypress', function (e) {
            var keycode = (e.keyCode ? e.keyCode : e.which);
            if(keycode == '13') {
              flag = true;
              $('#msg_popup').modal('hide');
            }
          });

          $("#msg_popup #confirm_yes").click(function() {
            flag = true;
            $('#msg_popup').modal('hide');

          });
          $("#msg_popup #confirm_no").click(function() {
            flag = false;
            $('#msg_popup').modal('hide');
          });

          $("#msg_popup").on("hidden.bs.modal", function(e) {
            e.stopPropagation();
            if(yeshandler != null && flag == true){
              resolve(1);
            }
            else if(nohandler != null && flag == false){
              resolve(2);
            }
            else reject();
          });
        }).then(function(value) {
          if(value == 1){
            yeshandler();
          }else if(value == 2){
            nohandler();
          }
        }).catch(function() {});
      },
    }

    function openWindowWithParams(baseURL, params, windowName, windowFeatures) {

        // 매개변수를 쿼리 문자열로 변환
        const queryString = Object.keys(params)
         .map(key => encodeURIComponent(key) + '=' + encodeURIComponent(params[key]))
         .join('&');

        // URL에 쿼리 문자열 추가
        const urlWithParams = `${baseURL}?${queryString}`;

        // 새 창 열기
        window.open(urlWithParams, windowName, windowFeatures);
    }


        function leftPad(value) {
            if (value >= 10) {
                return value;
            }
            return `0${value}`;
        }

        function toStringByFormatting(source, delimiter = '-') {
            var convert_date = new Date(source);
            const year = convert_date.getFullYear();
            const month = leftPad(convert_date.getMonth() + 1);
            const day = leftPad(convert_date.getDate());
            return [year, month, day].join(delimiter);
        }

    function decode(value, ...args) {
        // Arguments should be in pairs (search, result) and an optional default result at the end
        for (let i = 0; i < args.length - 1; i += 2) {
            if (value === args[i]) {
                return args[i + 1];
            }
        }
        // If no match is found, return the default value if provided
        return args.length % 2 === 1 ? args[args.length - 1] : null;
    }


    function roundDown(number,decimal) {
        return Math.floor(number / decimal) * decimal;
    }

    function roundUp(number,decimal) {
        return Math.ceil(number / decimal) * decimal;
    }


    function calculateDuration(startDate, endDate) {
        // 날짜 형식은 YYYY-MM-DD로 가정합니다.
        const start = new Date(startDate);
        const end = new Date(endDate);

        // 전체 일수 계산
        const totalDays = Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1;

        // 개월 수와 남은 일수 계산
        let months = end.getMonth() - start.getMonth() + (12 * (end.getFullYear() - start.getFullYear()));
        let days = (end.getDate() - start.getDate())+1;

        // 일수가 음수이면 한 달을 차감하고 일수 보정
        if (days < 0) {
            months--;
            const previousMonth = new Date(end.getFullYear(), end.getMonth(), 0).getDate();
            days += previousMonth;
        }

        // 시작일과 종료일이 월말에 걸치는 경우 일수 조정
        if (start.getDate() === 1 && end.getDate() === new Date(end.getFullYear(), end.getMonth() + 1, 0).getDate()) {
            months++;
            days = 0;
        }

        return `${months}개월 ${days}일 (${totalDays}일)`;
    }

    function validateRRN(rrn) {
        // 하이픈을 제거한 13자리 숫자만 남김
        const num = rrn.replace(/-/g, '');

        // 형식 확인
        if (num.length !== 13 || !/^\d{13}$/.test(num)) {
            return false;
        }

        // 외국인 등록번호의 경우 7번째 자리가 5, 6, 7, 8 중 하나여야 함
        const isForeigner = ['5', '6', '7', '8'].includes(num[6]);

        // 가중치 배열
        const weights = [2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5];
        let total = 0;

        // 가중치 합계 계산
        for (let i = 0; i < 12; i++) {
            total += parseInt(num[i], 10) * weights[i];
        }

        // 검증번호 계산
        const checkDigit = (11 - (total % 11)) % 10;
        return checkDigit === parseInt(num[12], 10);
    }

    function ufn_custom_key(search,save){

        document.addEventListener('keydown', function(event) {
            if (event.key=='F4' || event.key=='F8' ) {
                if(search){search();}
            }
            if(event.key=='F9'){if(save){save();}}
        });

    }




















