var Grid = tui.Grid;
Grid.setLanguage('ko');
Grid.applyTheme('custom', {
    selection: {
        background: 'rgba(26, 86, 219, 0.08)',
        border: '#1a56db'
    },
    scrollbar: {
        background: '#f1f5f9',
        thumb: '#cbd5e1',
        active: '#94a3b8'
    },
    row: {
        even: {
            background: '#f8fafc'
        },
        hover: {
            background: 'rgba(26, 86, 219, 0.04)'
        }
    },
    cell: {
        normal: {
            background: '#ffffff',
            border: '#e2e8f0',
            showVerticalBorder: false,
            text: '#0b1424'
        },
        header: {
            background: '#f8fafc',
            border: '#e2e8f0',
            showVerticalBorder: false,
            text: '#334155'
        },
        rowHeader: {
            border: '#e2e8f0',
            showVerticalBorder: false,
            text: '#94a3b8'
        },
        editable: {
            background: '#ffffff'
        },
        selectedHeader: {
            background: 'rgba(26, 86, 219, 0.08)'
        },
        focused: {
            border: '#1a56db'
        },
        disabled: {
            text: '#94a3b8'
        }
    },
    pagination: {
        background: '#ffffff',
        border: '#e2e8f0',
        text: '#334155'
    }
});

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
                filterInput.style.border = '1px solid #e2e8f0';
                filterInput.style.borderRadius = '8px';
                filterInput.style.padding = '8px';
                filterInput.style.fontSize = '0.875rem';
            }
        }, 100);
    });
});
