'use strict'

angular.module('kulebaoAdmin')
.controller 'ParentsCtrl',
    ['$scope', '$rootScope', 'parentService', '$stateParams', ($scope, $rootScope, parentService, $stateParams) ->
      $rootScope.tabName = 'parents'

      $scope.kindergarten = {
        school_id: $stateParams.kindergarten
      }
      $scope.parents = parentService.bind({school_id: $scope.kindergarten.school_id}).query()

      $scope.editingId = -1
      $scope.backupEditing = {}

      $scope.delete = (parent) ->
        $scope.parents = _.reject($scope.parents, (p) ->
          parent.id == p.id)
        parent.$delete()

      $scope.startEditing = (parent) ->
        $scope.editingId = parent.id
        $scope.backupEditing = angular.copy $scope.parents

      $scope.save = (parent) ->
        $scope.editingId = -1
        parent.$save()

      $scope.cancelEditing = () ->
        $scope.editingId = -1
        angular.copy $scope.backupEditing, $scope.parents
    ]


angular.module('kulebaoAdmin')
.controller 'AddAdultInfoCtrl', ['$scope', 'parentService', '$rootScope', '$location', ($scope, Parent, $rootScope, $location) ->
    if $rootScope.parent isnt undefined
      $scope.parent = $rootScope.parent
    else
      $scope.parent = new Parent({school_id: $scope.kindergarten.name})
      $scope.parent.birthday = new Date(10123123123)
      $scope.parent.gender = 1
      $scope.parent.portrait = '/assets/images/portrait_placeholder.png'
      $scope.parent.name = '马大帅'
      $scope.parent.kindergarten =
        name: '石家庄火车站幼儿园'
        school_id: 93740362
      $rootScope.parent = $scope.parent

    $scope.cancelCreating = ->
      $location.path($location.path().replace(/\/[^\/]+$/, '/list'))
      delete $rootScope.parent
      delete $rootScope.child
  ]

angular.module('kulebaoAdmin')
.controller 'AddChildInfoCtrl',
    ['$scope', 'parentService', '$rootScope', '$location', 'childService', ($scope, Parent, $rootScope, $location, Child) ->
      if $rootScope.parent is undefined
        $location.path($location.path().replace(/\/[^\/]+$/, '/list'))
      else if $rootScope.child isnt undefined
          $scope.child = $rootScope.child
      else
        $scope.child = new Child(
          birthday: new Date(931153123123)
          relationship: '妈妈'
          gender: 1
          portrait: '/assets/images/portrait_placeholder.png'
          parent: $rootScope.parent
          class_id: 101
          school_id: parseInt($rootScope.parent.kindergarten.school_id)
        )
        $rootScope.child = $scope.child

      $scope.kindergarten.classes = [
        {name: '西瓜班', id: 100},
        {name: '核桃班', id: 101}
      ]

      $scope.getClassName = (id) ->
        _.find($scope.kindergarten.classes, (c)-> c.id is id );

      $scope.cancelCreating = ->
        $location.path($location.path().replace(/\/[^\/]+$/, '/list'))
        delete $rootScope.parent
        delete $rootScope.child

      $scope.save = (child) ->
        child.$save(() -> $scope.cancelCreating())
    ]
