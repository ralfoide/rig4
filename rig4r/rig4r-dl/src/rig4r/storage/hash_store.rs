
#![allow(non_snake_case)]

use shaku::{Component, Interface};
use std::collections::HashMap;

pub trait IHashStore: Interface {
    fn putString(&mut self, description: &str, content: &str);
    fn getString(&self, description: &str) -> Option<&String>;
}

#[derive(Component)]
#[shaku(interface = IHashStore)]
pub struct HashStore {
    mCache: HashMap<String, String>
}

impl HashStore {
    fn new() -> impl IHashStore {
        HashStore{
            mCache: HashMap::new()
        }
    }
}

impl IHashStore for HashStore {
    fn putString(&mut self, description: &str, content: &str) {
        self.mCache.insert(String::from(description), String::from(content));
    }

    fn getString(&self, description: &str) -> Option<&String> {
        self.mCache.get(description)
    }
}

#[cfg(test)]
mod tests_hash_store {
    use super::*;
    use shaku::{module, HasComponent};

    module! {
        TestModule {
            components = [HashStore],
            providers = []
        }
    }

    #[test]
    fn test_putString_getString() {
        let mut hs = HashStore::new();
        assert_eq!(hs.getString("key"), None);

        hs.putString("key", "value");
        assert_eq!(hs.getString("key").unwrap(), "value");
        assert_eq!(hs.getString("key").unwrap(), "value");

        hs.putString("key", "value2");
        assert_eq!(hs.getString("key").unwrap(), "value2");
    }

    #[test]
    fn test_as_module() {
        let mut m = TestModule::builder().build();
        let hs: &mut dyn IHashStore = m.resolve_mut().unwrap();
        assert_eq!(hs.getString("key"), None);
        hs.putString("key", "value");
        assert_eq!(hs.getString("key").unwrap(), "value");
    }
}
