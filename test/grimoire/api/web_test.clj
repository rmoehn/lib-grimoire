(ns grimoire.api.web-test
  (:require [grimoire.api :as api]
            [grimoire.things :as t]
            [grimoire.either :refer [succeed? result]]
            [grimoire.api.web :refer [->Config]]
            [grimoire.api.web.read]
            [clojure.set :as set]
            [clojure.test :refer :all]))

(def test-config
  (->Config "http://127.0.0.1:3000"))

;; Listing tests
;;------------------------------------------------------------------------------

(deftest list-groups-test
  (let [?res (api/list-groups test-config)]
    (is (succeed? ?res))
    (doseq [?g (result ?res)]
      (is (t/group? ?g)))))

(deftest list-artifacts-test
  (let [g    (t/->Group "org.clojure")
        ?res (api/list-artifacts test-config g)]
    (is (succeed? ?res))
    (doseq [?a (result ?res)]
      (is (t/artifact? ?a)))))

(deftest list-versions-test
  (let [a    (-> (t/->Group "org.clojure")
                 (t/->Artifact "clojure"))
        ?res (api/list-versions test-config a)]
    (is (succeed? ?res))
    (doseq [?v (result ?res)]
      (is (t/version? ?v)))))

(deftest list-platform-test
  (let [v    (->  (t/->Group "org.clojure")
                  (t/->Artifact "clojure")
                  (t/->Version "1.6.0"))
        ?res (api/list-platforms test-config v)]
    (is (succeed? ?res))
    (doseq [?platform (result ?res)]
      (is (t/platform? ?platform)))))

(deftest list-ns-test
  (let [v    (->  (t/->Group "org.clojure")
                  (t/->Artifact "clojure")
                  (t/->Version "1.6.0")
                  (t/->Platform "clj"))
        ?res (api/list-namespaces test-config v)]
    (is (succeed? ?res))
    (doseq [?ns (result ?res)]
      (is (t/namespace? ?ns)))))

(deftest list-prior-versions-test
  (let [ns   (-> (t/->Group "org.clojure")
                 (t/->Artifact "clojure")
                 (t/->Version "1.6.0")
                 (t/->Platform "clj")
                 (t/->Ns "clojure.core"))
        ?res (api/thing->prior-versions test-config ns)]
    (is (succeed? ?res))
    (is (set/subset?
         #{"1.6.0" "1.5.0" "1.4.0"}
         (->> ?res result (map (comp t/thing->name t/thing->version)) set)))))

(deftest list-def-test
  (let [ns   (-> (t/->Group "org.clojure")
                 (t/->Artifact "clojure")
                 (t/->Version "1.6.0")
                 (t/->Platform "clj")
                 (t/->Ns "clojure.core"))
        ?res (api/list-defs test-config ns)]
    (is (succeed? ?res))
    (let [defs (->> ?res result (map t/thing->name) set)]
      (doseq [d ["for" "def" "let" "catch" "io!" "every?" "even?" "->>" "+" "-'"]]
        (is (defs d))))))

(deftest read-meta-test
  (let [plat (-> (t/->Group "org.clojure")
                 (t/->Artifact "clojure")
                 (t/->Version "1.6.0")
                 (t/->Platform "clj"))
        ?nss (api/list-namespaces test-config plat)]
    (is (succeed? ?nss))

    (doseq [ns (result ?nss)]
      (is (t/namespace? ns))
      (is (succeed? (api/read-meta test-config ns)))
      
      (let [?defs (api/list-defs test-config ns)]
        (is (succeed? ?defs))
        (doseq [d (result ?defs)]
          (is (succeed? (api/read-meta test-config d))))))))
