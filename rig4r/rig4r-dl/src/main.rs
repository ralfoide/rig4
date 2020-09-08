
#![allow(non_snake_case)]

use shaku::{module, HasComponent};

mod rig4r;
use rig4r::config::*;
use rig4r::storage::*;
use rig4r::utils::*;

module! {
    MainModule {
        components = [Flags, BlobStore, HashStore, FileOps],
        providers = []
    }
}

fn setupModule() -> MainModule {
    MainModule::builder().build()
}

fn main() {
    println!("Hello, world!");

    let mut mainModule = setupModule();
    
    let hs: &mut dyn IHashStore = mainModule.resolve_mut().unwrap();
    hs.putString("foo", "store");
    println!("Store {}", hs.getString("foo").unwrap());
}

#[cfg(test)]
mod tests_main {
    #[test]
    fn test1() {
        assert_eq!(2 + 2, 4);
    }
}
