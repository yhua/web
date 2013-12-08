// Generated by CoffeeScript 1.6.3
(function() {
  'use strict';
  var Controller;

  Controller = (function() {
    function Controller(newsService, readService) {
      var _this = this;
      this.kindergarten = {
        id: 1,
        name: 'school23'
      };
      this.adminUser = {
        id: 1,
        name: '豆瓣'
      };
      this.newsletters = newsService.bind({
        kg: this.kindergarten.name
      }).query(function() {
        var news, _i, _len, _ref, _results;
        _ref = _this.newsletters;
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          news = _ref[_i];
          _results.push((function(news) {
            return news.readCount = 100;
          })(news));
        }
        return _results;
      });
    }

    return Controller;

  })();

  angular.module('admin').controller('BulletinManageCtrl', ['newsService', 'readService', Controller]);

}).call(this);