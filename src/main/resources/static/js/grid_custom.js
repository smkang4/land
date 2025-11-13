var Grid = tui.Grid;
Grid.setLanguage('ko'); // set Korean
Grid.applyTheme('custom', {
    selection: {
        background: 'rgba(25, 118, 210, 0.08)',
        border: '#1976d2'
    },
    scrollbar: {
        background: '#f5f5f5',
        thumb: '#bdbdbd',
        active: '#9e9e9e'
    },
    row: {
        even: {
            background: '#ffffff'
        },
        hover: {
            background: '#f5f5f5'
        }
    },
    cell: {
        normal: {
            background: '#ffffff',
            border: 'rgba(0,0,0,0.08)',
            showVerticalBorder: true,
            text: '#333333'
        },
        header: {
            background: '#ffffff',
            border: 'rgba(0,0,0,0.08)',
            showVerticalBorder: true,
            text: '#1a1a1a'
        },
        rowHeader: {
            border: 'rgba(0,0,0,0.08)',
            showVerticalBorder: true,
            text: '#757575'
        },
        editable: {
            background: '#ffffff'
        },
        selectedHeader: {
            background: 'rgba(25, 118, 210, 0.08)'
        },
        focused: {
            border: '#1976d2'
        },
        disabled: {
            text: '#9e9e9e'
        }
    },
    pagination: {
        background: '#ffffff',
        border: 'rgba(0,0,0,0.08)',
        text: '#424242'
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
                
                // 필터 입력 필드 스타일 개선
                filterInput.style.transition = 'all 0.2s ease';
                filterInput.style.border = '1px solid rgba(0,0,0,0.12)';
                filterInput.style.borderRadius = '4px';
                filterInput.style.padding = '8px';
                filterInput.style.fontSize = '0.875rem';
                
                filterInput.addEventListener('keydown', function(e) {
                    if (e.key === "Enter") {
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
