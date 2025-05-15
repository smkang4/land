var Grid = tui.Grid;
Grid.setLanguage('ko'); // set Korean
Grid.applyTheme('custom',{
  selection: {
    background: '#4daaf9',
    border: '#004082'
  },
  scrollbar: {
    background: '#f5f5f5',
    thumb: '#d9d9d9',
    active: '#c1c1c1'
  },
  row: {
    even: {
      //background: '#f3ffe3'
    },
    hover: {
      //background: 'yellow'
    }
  },
  cell: {
    normal: {
      background: 'white',
      border: '#e0e0e0',
      showVerticalBorder: true
    },
    header: {
      background: '#eee',
      border: '#ccc',
      showVerticalBorder: true
    },
    rowHeader: {
      border: '#ccc',
      showVerticalBorder: true
    },
    editable: {
      background: '#fbfbfb'
    },
    selectedHeader: {
      background: '#d8d8d8'
    },
    focused: {
      border: '#418ed4'
    },
    disabled: {
      text: '#b0b0b0'
    }
  }
}); // Call API of static method

function setDefaultFilterCondition(grid, columnName, condition = 'contain') {
   grid.filter(columnName, [{ code: condition, value: '' }]);
   grid.on('afterUnfilter', (ev) => {
        grid.filter(columnName, [{ code: condition, value: '' }]);
   });
}

$(document).ready(function(){
    $(".tui-grid-btn-filter").on("click",function(e){
        $(".tui-grid-filter-dropdown>select").val("contain");
        setTimeout(() => {
            const filterInput = document.querySelector('.tui-grid-filter-input');
            if (filterInput) {
              filterInput.focus();

              filterInput.addEventListener('keydown', function(e) {
                  if (e.key === "Enter") {
                    // 엔터 키를 누르면 필터를 적용하고 팝업을 닫기
                    const closeButton = document.querySelector('.tui-grid-btn-close');
                    if (closeButton) {
                      closeButton.click();
                    }
                  }
                });
            }
        }, 100);
    });
});
