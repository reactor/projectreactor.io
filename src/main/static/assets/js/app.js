var links, codes;

function showFile(file) {
    if (!links) {
        return;
    }
    links.removeClass('active');
    codes.addClass('hide');
    
    $(links.get(file - 1)).addClass('active');
    $(codes.get(file - 1)).removeClass('hide');
}

$(function(){
    links = $("#files a");
    codes = $('#codes pre');

    var current = 0;
    var items = $("#code>.gist-code-wrap>.item");
    var ul = $("<ul />").addClass("steps");
    var max = items.length;

    for (var i=0; i<items.length; i++) {
        ul.append($("<li />").data('index', i).click(function(){
            show($(this).data('index'));
        }));
    }
    $($(ul).find('li').get(current)).addClass('active');
    $("#code").prepend(ul);

    function show(index) {
        var _this = $(ul.find('li').get(index));
        $(ul.find('li').get(current)).removeClass('active');
        $(items.get(current)).addClass('hide');
        $(items.get(_this.data('index'))).removeClass('hide');
        _this.addClass('active');
        current = _this.data('index');
    }

    function animateList() {
        setTimeout(function(){
            var index = current + 1;
            if (index >= max)
                index = 0;
            show(index);
            animateList();
        }, 10 * 1000);
    }

    animateList();

    /*$("#code>.gist-code-wrap>.item").each(function(){
        var ob = $(this);
        ob.addClass('hide');
    });*/
});